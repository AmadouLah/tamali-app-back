package com.tamali_app_back.www.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SaleItemRequest(
        @NotNull UUID productId,
        @Min(1) int quantity
) {}
