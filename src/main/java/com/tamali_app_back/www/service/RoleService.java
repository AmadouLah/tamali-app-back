package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.RoleDto;
import com.tamali_app_back.www.enums.RoleType;
import com.tamali_app_back.www.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<RoleDto> findAll() {
        return roleRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<RoleDto> getById(UUID id) {
        return roleRepository.findById(id).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<RoleDto> getByType(RoleType type) {
        return roleRepository.findByType(type).map(mapper::toDto);
    }
}
