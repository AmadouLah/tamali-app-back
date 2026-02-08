package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.StockMovementDto;
import com.tamali_app_back.www.entity.Product;
import com.tamali_app_back.www.entity.Stock;
import com.tamali_app_back.www.entity.StockMovement;
import com.tamali_app_back.www.enums.MovementType;
import com.tamali_app_back.www.repository.ProductRepository;
import com.tamali_app_back.www.repository.StockMovementRepository;
import com.tamali_app_back.www.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public Optional<StockMovementDto> getById(UUID id) {
        return stockMovementRepository.findById(id).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<StockMovementDto> findByProductId(UUID productId, int page, int size) {
        return stockMovementRepository.findByProductIdOrderByMovementAtDesc(productId, PageRequest.of(page, size))
                .stream().map(mapper::toDto).toList();
    }

    @Transactional
    public StockMovementDto create(UUID productId, int quantity, MovementType type) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return null;
        Stock stock = stockRepository.findByProductId(productId).orElse(null);
        if (stock == null) return null;
        if (type == MovementType.OUT || type == MovementType.SALE) {
            if (stock.getQuantity() < quantity) return null;
            stock.setQuantity(stock.getQuantity() - quantity);
        } else {
            stock.setQuantity(stock.getQuantity() + quantity);
        }
        stockRepository.save(stock);
        StockMovement mov = StockMovement.builder()
                .product(product)
                .quantity(type == MovementType.OUT || type == MovementType.SALE ? -quantity : quantity)
                .type(type)
                .movementAt(LocalDateTime.now())
                .build();
        return mapper.toDto(stockMovementRepository.save(mov));
    }
}
