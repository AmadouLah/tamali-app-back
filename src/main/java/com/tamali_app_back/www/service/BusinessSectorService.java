package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.BusinessSectorDto;
import com.tamali_app_back.www.entity.BusinessSector;
import com.tamali_app_back.www.repository.BusinessSectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessSectorService {

    private final BusinessSectorRepository repository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<BusinessSectorDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<BusinessSectorDto> findActive() {
        return repository.findByActiveTrue().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BusinessSectorDto getById(UUID id) {
        return repository.findById(id).map(mapper::toDto).orElse(null);
    }

    @Transactional
    public BusinessSectorDto create(String name, String description) {
        BusinessSector sector = BusinessSector.builder()
                .name(name)
                .description(description)
                .active(true)
                .build();
        return mapper.toDto(repository.save(sector));
    }

    @Transactional
    public BusinessSectorDto update(UUID id, String name, String description, Boolean active) {
        BusinessSector sector = repository.findById(id)
                .orElseThrow(() -> new com.tamali_app_back.www.exception.ResourceNotFoundException("Secteur d'activité", id));
        
        if (name != null) sector.setName(name);
        if (description != null) sector.setDescription(description);
        if (active != null) sector.setActive(active);
        
        return mapper.toDto(repository.save(sector));
    }

    @Transactional
    public void deleteById(UUID id) {
        if (!repository.existsById(id)) {
            throw new com.tamali_app_back.www.exception.ResourceNotFoundException("Secteur d'activité", id);
        }
        repository.deleteById(id);
    }
}
