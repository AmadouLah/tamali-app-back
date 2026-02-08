package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.TaxConfigurationDto;
import com.tamali_app_back.www.entity.Business;
import com.tamali_app_back.www.entity.TaxConfiguration;
import com.tamali_app_back.www.repository.BusinessRepository;
import com.tamali_app_back.www.repository.TaxConfigurationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaxConfigurationService {

    private final TaxConfigurationRepository taxConfigurationRepository;
    private final BusinessRepository businessRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public TaxConfigurationDto getByBusinessId(UUID businessId) {
        return taxConfigurationRepository.findByBusinessId(businessId).map(mapper::toDto).orElse(null);
    }

    @Transactional
    public TaxConfigurationDto saveOrUpdate(UUID businessId, boolean enabled, BigDecimal rate) {
        Business business = businessRepository.findById(businessId).orElse(null);
        if (business == null) return null;
        TaxConfiguration tc = taxConfigurationRepository.findByBusinessId(businessId).orElse(null);
        if (tc == null) {
            tc = TaxConfiguration.builder().business(business).enabled(enabled).rate(rate != null ? rate : BigDecimal.ZERO).build();
            business.setTaxConfiguration(tc);
        } else {
            tc.setEnabled(enabled);
            tc.setRate(rate != null ? rate : BigDecimal.ZERO);
        }
        return mapper.toDto(taxConfigurationRepository.save(tc));
    }
}
