package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductCreateRequest(
        @NotBlank String name,
        String reference,
        UUID categoryId,
        @NotNull BigDecimal unitPrice,
        boolean taxable,
        int initialQuantity
) {}
