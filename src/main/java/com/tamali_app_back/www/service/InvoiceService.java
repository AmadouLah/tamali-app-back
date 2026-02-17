package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.InvoiceDto;
import com.tamali_app_back.www.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public Optional<InvoiceDto> getById(UUID id) {
        return invoiceRepository.findById(id).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<InvoiceDto> getBySaleId(UUID saleId) {
        return invoiceRepository.findBySaleId(saleId).map(mapper::toDto);
    }

    @Transactional
    public void markSentByEmail(UUID invoiceId) {
        invoiceRepository.findById(invoiceId).ifPresent(i -> {
            i.setSentByEmail(true);
            invoiceRepository.save(i);
        });
    }

    @Transactional
    public void markSentByWhatsapp(UUID invoiceId) {
        invoiceRepository.findById(invoiceId).ifPresent(i -> {
            i.setSentByWhatsapp(true);
            invoiceRepository.save(i);
        });
    }
}
