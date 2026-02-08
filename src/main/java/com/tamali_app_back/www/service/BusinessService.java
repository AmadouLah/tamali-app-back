package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.BusinessDto;
import com.tamali_app_back.www.entity.Business;
import com.tamali_app_back.www.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<BusinessDto> findAll() {
        return businessRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BusinessDto getById(UUID id) {
        return businessRepository.findById(id).map(mapper::toDto).orElse(null);
    }

    @Transactional
    public BusinessDto create(String name, String email, String phone, String address) {
        Business b = Business.builder()
                .name(name)
                .email(email)
                .phone(phone)
                .address(address)
                .active(true)
                .build();
        return mapper.toDto(businessRepository.save(b));
    }

    @Transactional
    public BusinessDto update(UUID id, String name, String email, String phone, String address, Boolean active) {
        Business b = businessRepository.findById(id).orElse(null);
        if (b == null) return null;
        if (name != null) b.setName(name);
        if (email != null) b.setEmail(email);
        if (phone != null) b.setPhone(phone);
        if (address != null) b.setAddress(address);
        if (active != null) b.setActive(active);
        return mapper.toDto(businessRepository.save(b));
    }

    @Transactional
    public void deleteById(UUID id) {
        businessRepository.findById(id).ifPresent(b -> {
            b.setDeletedAt(LocalDateTime.now());
            businessRepository.save(b);
        });
    }
}
