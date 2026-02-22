package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    List<Sale> findByBusinessIdOrderBySaleDateDesc(UUID businessId, Pageable pageable);

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.business b LEFT JOIN FETCH b.receiptTemplate WHERE s.id = :id")
    Optional<Sale> findByIdWithBusinessAndTemplate(@Param("id") UUID id);
}
