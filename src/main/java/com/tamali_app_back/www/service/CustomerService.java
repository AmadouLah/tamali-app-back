package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.CustomerDetailsDto;
import com.tamali_app_back.www.dto.CustomerDto;
import com.tamali_app_back.www.dto.CustomerSummaryDto;
import com.tamali_app_back.www.dto.SaleDto;
import com.tamali_app_back.www.entity.Customer;
import com.tamali_app_back.www.repository.CustomerRepository;
import com.tamali_app_back.www.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<CustomerSummaryDto> findSummariesByBusinessId(UUID businessId) {
        return customerRepository.findByBusinessIdOrderByNameAsc(businessId).stream()
                .map(customer -> buildSummary(businessId, customer))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerDto> searchByName(UUID businessId, String q) {
        String query = q != null ? q.trim() : "";
        if (query.isBlank()) return List.of();
        return customerRepository.findByBusinessIdAndNameContainingIgnoreCaseOrderByNameAsc(businessId, query)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CustomerDetailsDto> getDetails(UUID businessId, UUID customerId) {
        return customerRepository.findById(customerId)
                .filter(c -> c.getBusiness() != null && businessId.equals(c.getBusiness().getId()))
                .map(customer -> {
                    List<SaleDto> sales = saleRepository.findByBusinessIdAndCustomerIdOrderBySaleDateDesc(businessId, customerId)
                            .stream()
                            .map(mapper::toDto)
                            .toList();
                    BigDecimal totalSpent = computeTotalSpent(sales);
                    return new CustomerDetailsDto(
                            customer.getId(),
                            customer.getName(),
                            customer.getPhone(),
                            sales.size(),
                            totalSpent,
                            sales
                    );
                });
    }

    private CustomerSummaryDto buildSummary(UUID businessId, Customer customer) {
        List<SaleDto> sales = saleRepository.findByBusinessIdAndCustomerIdOrderBySaleDateDesc(businessId, customer.getId())
                .stream()
                .map(mapper::toDto)
                .toList();
        BigDecimal totalSpent = computeTotalSpent(sales);
        return new CustomerSummaryDto(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                sales.size(),
                totalSpent
        );
    }

    private BigDecimal computeTotalSpent(List<SaleDto> sales) {
        return sales.stream()
                .map(SaleDto::totalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
