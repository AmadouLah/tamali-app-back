package com.tamali_app_back.www.dto;

import com.tamali_app_back.www.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentDto(
        UUID id,
        UUID saleId,
        BigDecimal amount,
        PaymentMethod method,
        LocalDateTime paymentDate
) {}
