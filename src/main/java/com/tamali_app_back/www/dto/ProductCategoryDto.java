package com.tamali_app_back.www.dto;

import java.util.UUID;

public record ProductCategoryDto(UUID id, String name, UUID businessId) {}
