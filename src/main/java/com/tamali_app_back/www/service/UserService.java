package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.UserDto;
import com.tamali_app_back.www.entity.Business;
import com.tamali_app_back.www.entity.Role;
import com.tamali_app_back.www.entity.User;
import com.tamali_app_back.www.enums.RoleType;
import com.tamali_app_back.www.exception.ResourceNotFoundException;
import com.tamali_app_back.www.repository.BusinessRepository;
import com.tamali_app_back.www.repository.RoleRepository;
import com.tamali_app_back.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int CODE_VALIDITY_MINUTES = 10;

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final RoleRepository roleRepository;
    private final MailService mailService;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public UserDto findByEmail(String email) {
        return userRepository.findByEmail(email).map(mapper::toDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public UserDto getById(UUID id) {
        return userRepository.findById(id).map(mapper::toDto).orElse(null);
    }

    @Transactional
    public UserDto create(String firstname, String lastname, String email, String password, UUID businessId, Set<RoleType> roleTypes) {
        if (userRepository.existsByEmail(email)) return null;
        Business business = businessId != null ? businessRepository.findById(businessId).orElse(null) : null;
        Set<Role> roles = roleTypes != null && !roleTypes.isEmpty()
                ? roleTypes.stream()
                .flatMap(rt -> roleRepository.findByType(rt).stream())
                .collect(java.util.stream.Collectors.toSet())
                : Set.of();
        User u = User.builder()
                .firstname(firstname)
                .lastname(lastname)
                .email(email)
                .password(password)
                .enabled(false)
                .business(business)
                .roles(roles)
                .build();
        return mapper.toDto(userRepository.save(u));
    }

    @Transactional
    public void setVerificationCode(UUID userId, String code, LocalDateTime expiration) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setVerificationCode(code);
            u.setCodeExpiration(expiration);
            userRepository.save(u);
        });
    }

    @Transactional
    public UserDto confirmCodeAndEnable(UUID userId, String code) {
        User u = userRepository.findById(userId).orElse(null);
        if (u == null || !code.equals(u.getVerificationCode()) || u.getCodeExpiration() == null || LocalDateTime.now().isAfter(u.getCodeExpiration()))
            return null;
        u.setVerificationCode(null);
        u.setCodeExpiration(null);
        u.setEnabled(true);
        return mapper.toDto(userRepository.save(u));
    }

    /**
     * Génère un code à 6 chiffres, le sauvegarde sur l'utilisateur et envoie l'email via Brevo (ou log).
     */
    @Transactional
    public UserDto requestLoginCode(String email) {
        User u = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte avec cet email."));
        String code = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        LocalDateTime exp = LocalDateTime.now().plusMinutes(CODE_VALIDITY_MINUTES);
        u.setVerificationCode(code);
        u.setCodeExpiration(exp);
        userRepository.save(u);
        mailService.sendVerificationCode(u.getEmail(), code, CODE_VALIDITY_MINUTES);
        return mapper.toDto(u);
    }
}
