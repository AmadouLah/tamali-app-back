package com.tamali_app_back.www.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CustomerSummaryDto(
        UUID id,
        String name,
        String phone,
        long purchasesCount,
        BigDecimal totalSpent
) {}
