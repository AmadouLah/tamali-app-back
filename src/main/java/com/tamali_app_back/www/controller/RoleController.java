package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.RoleDto;
import com.tamali_app_back.www.enums.RoleType;
import com.tamali_app_back.www.service.RoleService;
import com.tamali_app_back.www.util.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<RoleDto>> findAll() {
        return ResponseEntity.ok(roleService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDto> getById(@PathVariable UUID id) {
        RoleDto dto = ResponseUtil.requireFound(roleService.getById(id), "Rôle", id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<RoleDto> getByType(@PathVariable RoleType type) {
        RoleDto dto = ResponseUtil.requireFound(roleService.getByType(type), "Rôle", type);
        return ResponseEntity.ok(dto);
    }
}
