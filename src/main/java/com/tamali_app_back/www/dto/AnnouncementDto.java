package com.tamali_app_back.www.dto;

import java.util.UUID;

/**
 * DTO pour l'annonce globale affichée sur la page de connexion et les dashboards.
 */
public record AnnouncementDto(UUID id, String message) {}
