package com.tamali_app_back.www.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Résumé entreprise pour le Super Admin (liste, userCount, pas de détail ventes).
 */
public record BusinessSummaryDto(
        UUID id,
        String name,
        String email,
        boolean active,
        int userCount,
        LocalDateTime createdAt
) {}
