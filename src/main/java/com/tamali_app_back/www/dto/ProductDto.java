package com.tamali_app_back.www.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String reference,
        BigDecimal unitPrice,
        BigDecimal purchasePrice,
        UUID businessId,
        UUID categoryId,
        String categoryName,
        Integer stockQuantity,
        boolean taxable
) {}
