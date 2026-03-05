package com.tamali_app_back.www.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entrée du journal d'activités pour une entreprise.
 */
public record BusinessActivityEntryDto(
        String type,
        String action,
        UUID id,
        UUID businessId,
        UUID userId,
        String userDisplayName,
        LocalDateTime occurredAt,
        String syncStatus
) {
}

