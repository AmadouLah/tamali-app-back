package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {

    List<Sale> findByBusinessIdOrderBySaleDateDesc(UUID businessId, Pageable pageable);

    @Query("SELECT s FROM Sale s LEFT JOIN FETCH s.business b LEFT JOIN FETCH b.receiptTemplate WHERE s.id = :id")
    Optional<Sale> findByIdWithBusinessAndTemplate(@Param("id") UUID id);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s")
    BigDecimal sumTotalAmount();

    @Query("SELECT COUNT(DISTINCT s.business.id) FROM Sale s WHERE CAST(s.saleDate AS date) = CURRENT_DATE")
    long countDistinctBusinessIdsWithSaleToday();

    @Query(value = "SELECT CAST(s.sale_date AS date) as d, COUNT(*) as c FROM sales s WHERE s.sale_date >= :since AND s.deleted_at IS NULL GROUP BY CAST(s.sale_date AS date) ORDER BY d", nativeQuery = true)
    List<Object[]> countSalesPerDaySince(@Param("since") LocalDateTime since);

    @Query("SELECT s.business.id, COUNT(s) FROM Sale s WHERE s.saleDate >= :since GROUP BY s.business.id ORDER BY COUNT(s) DESC")
    List<Object[]> countSalesByBusinessSince(@Param("since") LocalDateTime since);

    @Query("SELECT s.business.id, MAX(s.saleDate) FROM Sale s GROUP BY s.business.id")
    List<Object[]> findLastSaleDateByBusinessId();
}
