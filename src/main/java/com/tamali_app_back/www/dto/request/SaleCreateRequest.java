package com.tamali_app_back.www.dto.request;

import com.tamali_app_back.www.enums.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SaleCreateRequest(
        @NotNull UUID cashierId,
        @NotEmpty @Valid List<SaleItemRequest> items,
        PaymentMethod method,
        String customerEmail,
        String customerPhone
) {}
