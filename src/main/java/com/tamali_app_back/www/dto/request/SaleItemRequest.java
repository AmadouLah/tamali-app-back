package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemRequest(
        @NotNull UUID productId,
        @DecimalMin(value = "0.1", inclusive = true) BigDecimal quantity
) {}
