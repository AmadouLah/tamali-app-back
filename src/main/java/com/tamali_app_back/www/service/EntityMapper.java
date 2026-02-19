package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.*;
import com.tamali_app_back.www.entity.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class EntityMapper {

    @PersistenceContext
    private EntityManager entityManager;

    public BusinessDto toDto(Business e) {
        if (e == null) return null;
        return new BusinessDto(
                e.getId(), e.getName(), e.getEmail(), e.getPhone(), e.getAddress(),
                e.getCountry(),
                e.getSector() != null ? e.getSector().getId() : null,
                e.getCommerceRegisterNumber(), e.getIdentificationNumber(),
                e.getLegalStatus(), e.getBankAccountNumber(), e.getWebsiteUrl(),
                e.isActive(), e.getReceiptTemplate() != null ? e.getReceiptTemplate().getId() : null,
                e.getLogoUrl(), e.getCreatedAt());
    }

    public BusinessSectorDto toDto(BusinessSector e) {
        if (e == null) return null;
        return new BusinessSectorDto(e.getId(), e.getName(), e.getDescription(), e.isActive(), e.getCreatedAt());
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
                e.getRoles() != null && !e.getRoles().isEmpty() 
                    ? e.getRoles().stream().map(this::toDto).collect(Collectors.toSet()) 
                    : java.util.Set.of(),
                e.isMustChangePassword()
        );
    }

    public ProductDto toDto(Product e) {
        if (e == null) return null;
        int qty = e.getStock() != null ? e.getStock().getQuantity() : 0;
        ProductCategory cat = e.getCategory();
        return new ProductDto(e.getId(), e.getName(), e.getReference(), e.getUnitPrice(), e.getPurchasePrice(),
                e.getBusiness() != null ? e.getBusiness().getId() : null,
                cat != null ? cat.getId() : null,
                cat != null ? cat.getName() : null,
                qty, e.isTaxable());
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
        UUID productId = null;
        String name = "Produit supprimé";
        
        // Récupérer directement depuis la DB pour éviter les problèmes de lazy loading avec produits supprimés
        // Cette approche évite complètement le problème car on ne passe pas par la relation Hibernate
        // La requête récupère l'ID et le nom même si le produit est soft-deleted (deleted_at IS NOT NULL)
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Object[]> results = entityManager.createNativeQuery(
                "SELECT si.product_id, " +
                "       CASE WHEN p.deleted_at IS NULL THEN p.name ELSE 'Produit supprimé' END as product_name " +
                "FROM sale_items si " +
                "LEFT JOIN products p ON si.product_id = p.id " +
                "WHERE si.id = :saleItemId"
            ).setParameter("saleItemId", e.getId())
             .getResultList();
            
            if (results != null && !results.isEmpty()) {
                Object[] result = results.get(0);
                if (result != null && result.length >= 2) {
                    productId = result[0] != null ? (UUID) result[0] : null;
                    String dbName = result[1] != null ? (String) result[1] : null;
                    if (dbName != null && !dbName.isEmpty()) {
                        name = dbName;
                    }
                }
            }
        } catch (Exception ex) {
            // En cas d'erreur, on garde les valeurs par défaut (productId null, name "Produit supprimé")
        }
        
        return new SaleItemDto(e.getId(), productId, name, e.getQuantity(), e.getPrice());
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
                e.getReceiptPdfUrl(), e.isSentByEmail(), e.isSentByWhatsapp(), e.getCreatedAt());
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
