package com.tamali_app_back.www.dto;

import com.tamali_app_back.www.enums.MovementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockMovementDto(
        UUID id,
        UUID productId,
        UUID businessId,
        UUID userId,
        String userDisplayName,
        BigDecimal quantity,
        MovementType type,
        LocalDateTime movementAt
) {
}
