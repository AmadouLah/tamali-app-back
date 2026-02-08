package com.tamali_app_back.www.dto;

import java.util.UUID;

public record StockDto(UUID id, UUID productId, int quantity) {}
