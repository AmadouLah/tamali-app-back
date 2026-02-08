package com.tamali_app_back.www.dto;

import com.tamali_app_back.www.enums.RoleType;

import java.util.UUID;

public record RoleDto(UUID id, RoleType type) {}
