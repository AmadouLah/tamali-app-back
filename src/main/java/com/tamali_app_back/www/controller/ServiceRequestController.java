package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.ServiceRequestDto;
import com.tamali_app_back.www.dto.request.ServiceRequestRequest;
import com.tamali_app_back.www.service.MailService;
import com.tamali_app_back.www.service.ServiceRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/service-requests")
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestController {

    private static final String DEFAULT_ADMIN_EMAIL = "amadoulandoure004@gmail.com";

    private final ServiceRequestService serviceRequestService;
    private final MailService mailService;

    @Value("${app.admin.emails:}")
    private String adminEmails;

    /**
     * Crée une demande d'utilisation du service et l'envoie par email au SUPER_ADMIN.
     */
    @PostMapping
    public ResponseEntity<ServiceRequestDto> createServiceRequest(@Valid @RequestBody ServiceRequestRequest request) {
        ServiceRequestDto savedRequest = serviceRequestService.create(request.email(), request.objective());
        
        List<String> adminEmailList = adminEmails != null && !adminEmails.trim().isEmpty()
                ? Arrays.stream(adminEmails.split(","))
                    .map(String::trim)
                    .filter(email -> !email.isEmpty())
                    .toList()
                : List.of(DEFAULT_ADMIN_EMAIL);
        
        if (adminEmailList.isEmpty()) {
            adminEmailList = List.of(DEFAULT_ADMIN_EMAIL);
        }
        
        log.info("Envoi de demande de service de {} à {}", request.email(), adminEmailList);
        
        adminEmailList.forEach(adminEmail -> {
            try {
                mailService.sendServiceRequest(request.email(), request.objective(), adminEmail);
                log.debug("Demande de service envoyée avec succès à {}", adminEmail);
            } catch (Exception e) {
                log.error("Échec envoi demande de service à {}: {}", adminEmail, e.getMessage(), e);
            }
        });
        
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRequest);
    }

    /**
     * Récupère toutes les demandes de service (pour le SUPER_ADMIN).
     */
    @GetMapping
    public ResponseEntity<List<ServiceRequestDto>> getAllServiceRequests() {
        return ResponseEntity.ok(serviceRequestService.findAll());
    }

    /**
     * Récupère les demandes en attente (non traitées).
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ServiceRequestDto>> getPendingServiceRequests() {
        return ResponseEntity.ok(serviceRequestService.findPending());
    }

    /**
     * Marque une demande comme traitée.
     */
    @PatchMapping("/{id}/process")
    public ResponseEntity<ServiceRequestDto> markAsProcessed(@PathVariable UUID id) {
        return ResponseEntity.ok(serviceRequestService.markAsProcessed(id));
    }
}
