package com.tamali_app_back.www.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record InvoiceDto(
        UUID id,
        UUID saleId,
        String invoiceNumber,
        String customerEmail,
        String customerPhone,
        boolean sentByEmail,
        boolean sentByWhatsapp,
        LocalDateTime createdAt
) {}
