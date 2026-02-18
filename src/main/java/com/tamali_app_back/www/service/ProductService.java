package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.ProductDto;
import com.tamali_app_back.www.dto.StockMovementDto;
import com.tamali_app_back.www.entity.*;
import com.tamali_app_back.www.enums.MovementType;
import com.tamali_app_back.www.repository.BusinessRepository;
import com.tamali_app_back.www.repository.ProductRepository;
import com.tamali_app_back.www.repository.StockMovementRepository;
import com.tamali_app_back.www.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockMovementService stockMovementService;
    private final BusinessRepository businessRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public List<ProductDto> findByBusinessId(UUID businessId) {
        return productRepository.findByBusinessId(businessId).stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ProductDto getById(UUID id) {
        return productRepository.findById(id).map(mapper::toDto).orElse(null);
    }

    @Transactional
    public ProductDto create(UUID businessId, String name, String reference, BigDecimal unitPrice, boolean taxable, int initialQuantity) {
        Business business = businessRepository.findById(businessId).orElse(null);
        if (business == null) return null;
        Product p = Product.builder()
                .name(name)
                .reference(reference)
                .unitPrice(unitPrice != null ? unitPrice : BigDecimal.ZERO)
                .business(business)
                .taxable(taxable)
                .build();
        p = productRepository.save(p);
        Stock s = Stock.builder().product(p).quantity(Math.max(0, initialQuantity)).build();
        s = stockRepository.saveAndFlush(s);
        p.setStock(s);
        if (initialQuantity > 0) {
            StockMovement m = StockMovement.builder()
                    .product(p).quantity(initialQuantity).type(MovementType.IN).movementAt(LocalDateTime.now()).build();
            stockMovementRepository.save(m);
        }
        return mapper.toDto(p);
    }

    @Transactional
    public ProductDto update(UUID id, String name, String reference, BigDecimal unitPrice, Boolean taxable) {
        Product p = productRepository.findById(id).orElse(null);
        if (p == null) return null;
        if (name != null) p.setName(name);
        if (reference != null) p.setReference(reference);
        if (unitPrice != null) p.setUnitPrice(unitPrice);
        if (taxable != null) p.setTaxable(taxable);
        return mapper.toDto(productRepository.save(p));
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> getMovements(UUID productId, int limit) {
        return stockMovementService.findByProductId(productId, 0, limit);
    }

    @Transactional
    public void deleteById(UUID id) {
        productRepository.findById(id).ifPresent(p -> {
            p.setDeletedAt(LocalDateTime.now());
            productRepository.save(p);
        });
    }
}
