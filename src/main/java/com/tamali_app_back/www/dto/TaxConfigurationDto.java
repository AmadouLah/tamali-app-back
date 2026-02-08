package com.tamali_app_back.www.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TaxConfigurationDto(UUID id, UUID businessId, boolean enabled, BigDecimal rate) {}
