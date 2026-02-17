package com.tamali_app_back.www.dto;

public record AuthResponse(
        String token,
        UserDto user
) {}
