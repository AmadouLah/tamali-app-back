package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank String name,
        String reference,
        @NotNull BigDecimal unitPrice,
        boolean taxable,
        int initialQuantity
) {}
