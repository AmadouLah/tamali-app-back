package com.tamali_app_back.www.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String reference,
        BigDecimal unitPrice,
        UUID businessId,
        Integer stockQuantity,
        boolean taxable
) {}
