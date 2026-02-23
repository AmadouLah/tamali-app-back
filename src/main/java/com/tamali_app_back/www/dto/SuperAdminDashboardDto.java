package com.tamali_app_back.www.dto;

/**
 * Agrégat des données du tableau de bord Super Admin (évite 4 appels séparés).
 */
public record SuperAdminDashboardDto(
        SuperAdminPlatformStatsDto platformStats,
        SuperAdminRecentActivityDto recentActivity,
        SuperAdminUsageStatsDto usageStats,
        SuperAdminSystemMonitoringDto systemMonitoring
) {}
