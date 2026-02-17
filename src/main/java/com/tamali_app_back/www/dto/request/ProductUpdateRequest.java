package com.tamali_app_back.www.dto.request;

import java.math.BigDecimal;

public record ProductUpdateRequest(
        String name,
        String reference,
        BigDecimal unitPrice,
        Boolean taxable
) {}
