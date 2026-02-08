package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.PaymentDto;
import com.tamali_app_back.www.entity.Payment;
import com.tamali_app_back.www.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public Optional<PaymentDto> getById(UUID id) {
        return paymentRepository.findById(id).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentDto> getBySaleId(UUID saleId) {
        return paymentRepository.findBySaleId(saleId).map(mapper::toDto);
    }
}
