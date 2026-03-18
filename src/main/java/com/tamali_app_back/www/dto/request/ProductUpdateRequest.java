package com.tamali_app_back.www.dto.request;

import com.tamali_app_back.www.enums.ProductType;
import com.tamali_app_back.www.enums.ProductUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductUpdateRequest(
        String name,
        String reference,
        UUID categoryId,
        BigDecimal unitPrice,
        BigDecimal purchasePrice,
        ProductType productType,
        ProductUnit unit,
        Boolean taxable
) {}
