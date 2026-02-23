package com.tamali_app_back.www.dto;

import java.util.List;

/**
 * Activité récente pour le Super Admin.
 */
public record SuperAdminRecentActivityDto(
        int newBusinessesCount,
        int newUsersCount,
        List<BusinessActivitySummaryDto> mostActiveBusinesses,
        List<BusinessActivitySummaryDto> inactiveBusinesses
) {}
