package com.tamali_app_back.www.dto;

import java.util.UUID;

/**
 * Résumé d'activité d'une entreprise (sans détail des ventes).
 */
public record BusinessActivitySummaryDto(
        UUID id,
        String name,
        long saleCountOrDaysSinceLastSale
) {}
