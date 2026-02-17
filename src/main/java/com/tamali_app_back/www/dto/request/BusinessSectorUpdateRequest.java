package com.tamali_app_back.www.dto.request;

public record BusinessSectorUpdateRequest(
        String name,
        String description,
        Boolean active
) {}
