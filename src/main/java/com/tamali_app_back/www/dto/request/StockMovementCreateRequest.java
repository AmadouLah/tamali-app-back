package com.tamali_app_back.www.dto.request;

import com.tamali_app_back.www.enums.MovementType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record StockMovementCreateRequest(
        @DecimalMin(value = "0.1", inclusive = true) BigDecimal quantity,
        @NotNull MovementType type,
        UUID userId
) {}
