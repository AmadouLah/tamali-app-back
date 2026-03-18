package com.tamali_app_back.www.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.tamali_app_back.www.enums.ProductType;
import com.tamali_app_back.www.enums.ProductUnit;

public record ProductDto(
        UUID id,
        String name,
        String reference,
        BigDecimal unitPrice,
        BigDecimal purchasePrice,
        ProductType productType,
        ProductUnit unit,
        UUID businessId,
        UUID categoryId,
        String categoryName,
        BigDecimal stockQuantity,
        boolean taxable
) {}
