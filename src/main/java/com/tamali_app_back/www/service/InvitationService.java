package com.tamali_app_back.www.service;

import com.tamali_app_back.www.entity.Invitation;
import com.tamali_app_back.www.entity.Role;
import com.tamali_app_back.www.entity.User;
import com.tamali_app_back.www.enums.RoleType;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.repository.InvitationRepository;
import com.tamali_app_back.www.repository.RoleRepository;
import com.tamali_app_back.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private static final int TOKEN_LENGTH = 32;
    private static final int VALIDITY_DAYS = 7;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public Invitation createInvitation(String email, User createdBy) {
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new BadRequestException("Un compte existe déjà avec cet email.");
        }

        if (invitationRepository.findByEmailAndUsedFalse(email).isPresent()) {
            throw new BadRequestException("Une invitation en attente existe déjà pour cet email.");
        }

        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(VALIDITY_DAYS);

        Invitation invitation = Invitation.builder()
                .email(email)
                .token(token)
                .expiresAt(expiresAt)
                .used(false)
                .createdBy(createdBy)
                .build();

        invitation = invitationRepository.save(invitation);

        String invitationLink = frontendUrl + "invitation/" + token;
        mailService.sendInvitation(email, invitationLink, VALIDITY_DAYS);

        log.info("Invitation créée pour {} par {}", email, createdBy.getEmail());
        return invitation;
    }

    @Transactional
    public User acceptInvitation(String token, String password, String firstname, String lastname) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Lien d'invitation invalide."));

        if (invitation.isUsed()) {
            throw new BadRequestException("Cette invitation a déjà été utilisée.");
        }

        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cette invitation a expiré.");
        }

        if (userRepository.findByEmailIgnoreCase(invitation.getEmail()).isPresent()) {
            throw new BadRequestException("Un compte existe déjà avec cet email.");
        }

        Role businessOwnerRole = roleRepository.findByType(RoleType.BUSINESS_OWNER)
                .orElseThrow(() -> new IllegalStateException("Rôle BUSINESS_OWNER introuvable."));

        User user = User.builder()
                .email(invitation.getEmail())
                .firstname(firstname)
                .lastname(lastname)
                .password(passwordEncoder.encode(password))
                .enabled(true)
                .roles(Set.of(businessOwnerRole))
                .business(null)
                .build();

        user = userRepository.save(user);
        invitation.setUsed(true);
        invitationRepository.save(invitation);

        log.info("Invitation acceptée par {} (token: {})", invitation.getEmail(), token);
        return user;
    }

    @Transactional(readOnly = true)
    public Invitation getByToken(String token) {
        return invitationRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Lien d'invitation invalide."));
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
