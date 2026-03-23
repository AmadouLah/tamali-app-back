package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.CustomerDetailsDto;
import com.tamali_app_back.www.dto.CustomerDto;
import com.tamali_app_back.www.dto.CustomerSummaryDto;
import com.tamali_app_back.www.dto.SaleDto;
import com.tamali_app_back.www.entity.Invoice;
import com.tamali_app_back.www.entity.Payment;
import com.tamali_app_back.www.entity.Sale;
import com.tamali_app_back.www.entity.Customer;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.repository.CustomerRepository;
import com.tamali_app_back.www.repository.InvoiceRepository;
import com.tamali_app_back.www.repository.PaymentRepository;
import com.tamali_app_back.www.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
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
        return customerRepository.findByIdAndBusinessId(customerId, businessId)
                .map(customer -> {
                    List<SaleDto> sales = getSalesDtos(businessId, customerId);
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

    @Transactional
    public Optional<CustomerDto> update(UUID businessId, UUID customerId, String name, String phone) {
        return customerRepository.findByIdAndBusinessId(customerId, businessId)
                .map(customer -> {
                    if (name != null) {
                        String normalizedName = name.trim();
                        if (normalizedName.isBlank()) {
                            throw new BadRequestException("Le nom du client ne peut pas être vide.");
                        }
                        customer.setName(normalizedName);
                    }
                    if (phone != null) {
                        String normalizedPhone = phone.trim();
                        customer.setPhone(normalizedPhone.isBlank() ? null : normalizedPhone);
                    }
                    return mapper.toDto(customerRepository.save(customer));
                });
    }

    @Transactional
    public boolean deleteWithSales(UUID businessId, UUID customerId) {
        Optional<Customer> maybeCustomer = customerRepository.findByIdAndBusinessId(customerId, businessId);
        if (maybeCustomer.isEmpty()) return false;

        Customer customer = maybeCustomer.get();
        LocalDateTime now = LocalDateTime.now();
        List<Sale> sales = saleRepository.findByBusinessIdAndCustomerIdOrderBySaleDateDesc(businessId, customerId);
        List<UUID> saleIds = sales.stream().map(Sale::getId).toList();

        if (!saleIds.isEmpty()) {
            List<Payment> payments = paymentRepository.findBySaleIdIn(saleIds);
            payments.forEach(payment -> payment.setDeletedAt(now));
            paymentRepository.saveAll(payments);

            List<Invoice> invoices = invoiceRepository.findBySaleIdIn(saleIds);
            invoices.forEach(invoice -> invoice.setDeletedAt(now));
            invoiceRepository.saveAll(invoices);

            sales.forEach(sale -> sale.setDeletedAt(now));
            saleRepository.saveAll(sales);
        }

        customer.setDeletedAt(now);
        customerRepository.save(customer);
        return true;
    }

    private CustomerSummaryDto buildSummary(UUID businessId, Customer customer) {
        List<SaleDto> sales = getSalesDtos(businessId, customer.getId());
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

    private List<SaleDto> getSalesDtos(UUID businessId, UUID customerId) {
        return saleRepository.findByBusinessIdAndCustomerIdOrderBySaleDateDesc(businessId, customerId)
                .stream()
                .map(mapper::toDto)
                .toList();
    }
}
