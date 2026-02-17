package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.ServiceRequestDto;
import com.tamali_app_back.www.entity.ServiceRequest;
import com.tamali_app_back.www.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository repository;
    private final EntityMapper mapper;

    @Transactional
    public ServiceRequestDto create(String email, String objective) {
        ServiceRequest request = ServiceRequest.builder()
                .email(email.trim().toLowerCase())
                .objective(objective.trim())
                .processed(false)
                .build();
        return mapper.toDto(repository.save(request));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestDto> findAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestDto> findPending() {
        return repository.findByProcessedFalseOrderByCreatedAtDesc().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public ServiceRequestDto markAsProcessed(UUID id) {
        ServiceRequest request = repository.findById(id)
                .orElseThrow(() -> new com.tamali_app_back.www.exception.ResourceNotFoundException("Demande de service", id));
        request.setProcessed(true);
        return mapper.toDto(repository.save(request));
    }
}
