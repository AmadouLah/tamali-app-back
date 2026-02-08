package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    List<StockMovement> findByProductIdOrderByMovementAtDesc(UUID productId, Pageable pageable);
}
