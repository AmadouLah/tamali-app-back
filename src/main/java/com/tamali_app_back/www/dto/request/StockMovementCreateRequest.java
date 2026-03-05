package com.tamali_app_back.www.dto.request;

import com.tamali_app_back.www.enums.MovementType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockMovementCreateRequest(
        @Min(1) int quantity,
        @NotNull MovementType type,
        UUID userId
) {}
