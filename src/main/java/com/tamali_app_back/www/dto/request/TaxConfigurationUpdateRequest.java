package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TaxConfigurationUpdateRequest(
        boolean enabled,
        @NotNull BigDecimal rate
) {}
