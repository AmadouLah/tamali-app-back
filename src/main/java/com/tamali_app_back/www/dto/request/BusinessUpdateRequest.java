package com.tamali_app_back.www.dto.request;

public record BusinessUpdateRequest(
        String name,
        String email,
        String phone,
        String address,
        Boolean active
) {}
