package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.*;
import com.tamali_app_back.www.entity.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class EntityMapper {

    public BusinessDto toDto(Business e) {
        if (e == null) return null;
        return new BusinessDto(e.getId(), e.getName(), e.getEmail(), e.getPhone(), e.getAddress(),
                e.isActive(), e.getReceiptTemplate() != null ? e.getReceiptTemplate().getId() : null,
                e.getLogoUrl(), e.getCreatedAt());
    }

    public RoleDto toDto(Role e) {
        if (e == null) return null;
        return new RoleDto(e.getId(), e.getType());
    }

    public UserDto toDto(User e) {
        if (e == null) return null;
        return new UserDto(
                e.getId(), e.getFirstname(), e.getLastname(), e.getEmail(), e.isEnabled(),
                e.getBusiness() != null ? e.getBusiness().getId() : null,
                e.getRoles() != null ? e.getRoles().stream().map(this::toDto).collect(Collectors.toSet()) : null
        );
    }

    public ProductDto toDto(Product e) {
        if (e == null) return null;
        int qty = e.getStock() != null ? e.getStock().getQuantity() : 0;
        return new ProductDto(e.getId(), e.getName(), e.getReference(), e.getUnitPrice(),
                e.getBusiness() != null ? e.getBusiness().getId() : null, qty, e.isTaxable());
    }

    public StockDto toDto(Stock e) {
        if (e == null) return null;
        return new StockDto(e.getId(), e.getProduct() != null ? e.getProduct().getId() : null, e.getQuantity());
    }

    public StockMovementDto toDto(StockMovement e) {
        if (e == null) return null;
        return new StockMovementDto(e.getId(),
                e.getProduct() != null ? e.getProduct().getId() : null,
                e.getQuantity(), e.getType(), e.getMovementAt());
    }

    public SaleItemDto toDto(SaleItem e) {
        if (e == null) return null;
        String name = e.getProduct() != null ? e.getProduct().getName() : null;
        return new SaleItemDto(e.getId(), e.getProduct() != null ? e.getProduct().getId() : null, name, e.getQuantity(), e.getPrice());
    }

    public SaleDto toDto(Sale e) {
        if (e == null) return null;
        var items = e.getItems() != null ? e.getItems().stream().map(this::toDto).toList() : null;
        return new SaleDto(e.getId(),
                e.getBusiness() != null ? e.getBusiness().getId() : null,
                e.getCashier() != null ? e.getCashier().getId() : null,
                items, e.getTotalAmount(), e.getTaxAmount(), e.getSaleDate());
    }

    public InvoiceDto toDto(Invoice e) {
        if (e == null) return null;
        return new InvoiceDto(e.getId(), e.getSale() != null ? e.getSale().getId() : null,
                e.getInvoiceNumber(), e.getCustomerEmail(), e.getCustomerPhone(),
                e.isSentByEmail(), e.isSentByWhatsapp(), e.getCreatedAt());
    }

    public PaymentDto toDto(Payment e) {
        if (e == null) return null;
        return new PaymentDto(e.getId(), e.getSale() != null ? e.getSale().getId() : null,
                e.getAmount(), e.getMethod(), e.getPaymentDate());
    }

    public TaxConfigurationDto toDto(TaxConfiguration e) {
        if (e == null) return null;
        return new TaxConfigurationDto(e.getId(), e.getBusiness() != null ? e.getBusiness().getId() : null, e.isEnabled(), e.getRate());
    }

    public ReceiptTemplateDto toDto(ReceiptTemplate e) {
        if (e == null) return null;
        return new ReceiptTemplateDto(e.getId(), e.getCode(), e.getName(), e.getHtmlContent(),
                e.getCssContent(), e.isDefault(), e.isActive(), e.getCreatedAt());
    }

    public InvitationDto toDto(Invitation e) {
        if (e == null) return null;
        return new InvitationDto(e.getId(), e.getEmail(), e.getExpiresAt(), e.isUsed(), e.getCreatedAt());
    }

    public ServiceRequestDto toDto(ServiceRequest e) {
        if (e == null) return null;
        return new ServiceRequestDto(e.getId(), e.getEmail(), e.getObjective(), e.isProcessed(), e.getCreatedAt());
    }
}
