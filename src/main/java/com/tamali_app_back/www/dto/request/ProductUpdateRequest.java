package com.tamali_app_back.www.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpdateRequest(
        String name,
        String reference,
        UUID categoryId,
        BigDecimal unitPrice,
        BigDecimal purchasePrice,
        Boolean taxable
) {}
