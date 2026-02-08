package com.tamali_app_back.www.dto;

import com.tamali_app_back.www.enums.MovementType;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementDto(
        UUID id,
        UUID productId,
        int quantity,
        MovementType type,
        LocalDateTime movementAt
) {}
