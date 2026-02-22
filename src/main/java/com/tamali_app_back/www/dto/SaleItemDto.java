package com.tamali_app_back.www.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleItemDto(
        UUID id,
        UUID productId,
        String productName,
        int quantity,
        BigDecimal price,
        BigDecimal purchasePrice
) {}
