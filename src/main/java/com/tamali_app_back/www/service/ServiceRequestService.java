package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.ServiceRequestDto;
import com.tamali_app_back.www.entity.ServiceRequest;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository repository;
    private final EntityMapper mapper;

    private static final String PASSWORD_RESET_OBJECTIVE = "Demande de réinitialisation du mot de passe";
    private static final int PASSWORD_RESET_COOLDOWN_MINUTES = 5;

    @Transactional
    public ServiceRequestDto create(String email, String objective) {
        String normalizedEmail = email.trim().toLowerCase();
        String normalizedObjective = objective.trim();

        if (isPasswordResetObjective(normalizedObjective)) {
            enforcePasswordResetCooldown(normalizedEmail);
            normalizedObjective = PASSWORD_RESET_OBJECTIVE + " pour le compte: " + normalizedEmail;
        }

        ServiceRequest request = ServiceRequest.builder()
                .email(normalizedEmail)
                .objective(normalizedObjective)
                .processed(false)
                .build();
        return mapper.toDto(repository.save(request));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestDto> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestDto> findPending() {
        return repository.findByProcessedFalseOrderByCreatedAtDesc().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public ServiceRequestDto markAsProcessed(UUID id) {
        ServiceRequest request = repository.findById(id)
                .orElseThrow(() -> new com.tamali_app_back.www.exception.ResourceNotFoundException("Demande de service", id));
        request.setProcessed(true);
        return mapper.toDto(repository.save(request));
    }

    private static boolean isPasswordResetObjective(String objective) {
        return objective != null && objective.toLowerCase().contains("réinitialisation") && objective.toLowerCase().contains("mot de passe");
    }

    private void enforcePasswordResetCooldown(String normalizedEmail) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(PASSWORD_RESET_COOLDOWN_MINUTES);
        long recent = repository.countRecentByEmailAndObjective(
                normalizedEmail,
                PASSWORD_RESET_OBJECTIVE + " pour le compte: " + normalizedEmail,
                since
        );
        if (recent > 0) {
            throw new BadRequestException("Une demande de réinitialisation a déjà été envoyée. Réessayez dans 5 minutes.");
        }
    }
}
