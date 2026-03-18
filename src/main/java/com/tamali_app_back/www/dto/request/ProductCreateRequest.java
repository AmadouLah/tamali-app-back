package com.tamali_app_back.www.dto.request;

import com.tamali_app_back.www.enums.ProductType;
import com.tamali_app_back.www.enums.ProductUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCreateRequest(
        @NotBlank String name,
        String reference,
        UUID categoryId,
        @NotNull BigDecimal unitPrice,
        BigDecimal purchasePrice,
        ProductType productType,
        ProductUnit unit,
        boolean taxable,
        @NotNull BigDecimal initialQuantity
) {}
