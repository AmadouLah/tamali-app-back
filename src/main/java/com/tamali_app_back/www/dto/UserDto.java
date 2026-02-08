package com.tamali_app_back.www.dto;

import java.util.Set;
import java.util.UUID;

public record UserDto(
        UUID id,
        String firstname,
        String lastname,
        String email,
        boolean enabled,
        UUID businessId,
        Set<RoleDto> roles
) {}
