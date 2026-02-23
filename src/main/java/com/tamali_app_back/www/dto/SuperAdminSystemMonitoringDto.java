package com.tamali_app_back.www.dto;

import java.util.List;

/**
 * Monitoring système pour le Super Admin (stabilité).
 */
public record SuperAdminSystemMonitoringDto(
        String serverStatus,
        List<String> criticalErrors,
        List<String> syncFailures,
        List<String> emailOrWhatsAppFailures
) {}
