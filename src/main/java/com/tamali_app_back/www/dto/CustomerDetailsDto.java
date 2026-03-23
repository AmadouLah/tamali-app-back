package com.tamali_app_back.www.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CustomerDetailsDto(
        UUID id,
        String name,
        String phone,
        long purchasesCount,
        BigDecimal totalSpent,
        List<SaleDto> sales
) {}
