package com.tamali_app_back.www.dto;

import java.math.BigDecimal;

/**
 * Vue globale plateforme pour le Super Admin (macro uniquement).
 */
public record SuperAdminPlatformStatsDto(
        long totalBusinesses,
        long totalUsers,
        long totalSalesCount,
        BigDecimal totalTransactionVolume,
        long activeBusinessesToday
) {}
