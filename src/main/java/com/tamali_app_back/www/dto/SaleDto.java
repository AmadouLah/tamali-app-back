package com.tamali_app_back.www.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SaleDto(
        UUID id,
        UUID businessId,
        UUID cashierId,
        List<SaleItemDto> items,
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        LocalDateTime saleDate
) {}
