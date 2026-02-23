package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Requête pour créer ou mettre à jour le message d'annonce affiché sur l'application.
 */
public record AnnouncementRequest(@NotBlank(message = "Le message est requis") String message) {}
