package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Requête pour envoyer un email à tous les utilisateurs (Super Admin).
 */
public record BroadcastEmailRequest(
        @NotBlank(message = "Le sujet est requis") String subject,
        @NotBlank(message = "Le message est requis") String message
) {}
