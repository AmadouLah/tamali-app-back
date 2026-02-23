package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    @Query("SELECT COUNT(b) FROM Business b WHERE b.createdAt >= :since")
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);
}
