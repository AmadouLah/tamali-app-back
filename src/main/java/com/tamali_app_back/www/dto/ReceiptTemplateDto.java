package com.tamali_app_back.www.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReceiptTemplateDto(
        UUID id,
        String code,
        String name,
        String htmlContent,
        String cssContent,
        boolean isDefault,
        boolean active,
        LocalDateTime createdAt
) {}
