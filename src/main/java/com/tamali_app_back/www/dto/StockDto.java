package com.tamali_app_back.www.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record StockDto(UUID id, UUID productId, BigDecimal quantity) {}
