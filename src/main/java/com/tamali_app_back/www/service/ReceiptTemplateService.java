package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.ReceiptTemplateDto;
import com.tamali_app_back.www.entity.ReceiptTemplate;
import com.tamali_app_back.www.repository.ReceiptTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReceiptTemplateService {

    private final ReceiptTemplateRepository repository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<ReceiptTemplateDto> findAllActive() {
        return repository.findByActiveTrue().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ReceiptTemplateDto getById(UUID id) {
        return repository.findById(id).map(mapper::toDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public ReceiptTemplateDto getByCode(String code) {
        return repository.findByCode(code).map(mapper::toDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public ReceiptTemplate getEntityById(UUID id) {
        return repository.findById(id).orElse(null);
    }

    ReceiptTemplateRepository getRepository() {
        return repository;
    }
}
