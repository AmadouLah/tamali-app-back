package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.StockDto;
import com.tamali_app_back.www.entity.Stock;
import com.tamali_app_back.www.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final EntityMapper mapper;

    @Transactional(readOnly = true)
    public Optional<StockDto> getById(UUID id) {
        return stockRepository.findById(id).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<StockDto> getByProductId(UUID productId) {
        return stockRepository.findByProductId(productId).map(mapper::toDto);
    }
}
