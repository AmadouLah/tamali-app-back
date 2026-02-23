package com.tamali_app_back.www.dto;

import java.util.List;

/**
 * Statistiques d'utilisation plateforme (ventes/jour, pic, taux).
 */
public record SuperAdminUsageStatsDto(
        List<SalesPerDayDto> salesPerDay,
        String peakActivityLabel,
        double usageRatePercent
) {}
