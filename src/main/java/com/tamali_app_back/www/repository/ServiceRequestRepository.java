package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByProcessedFalseOrderByCreatedAtDesc();

    List<ServiceRequest> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(sr) FROM ServiceRequest sr WHERE sr.email = :email AND sr.objective = :objective AND sr.createdAt >= :since")
    long countRecentByEmailAndObjective(@Param("email") String email, @Param("objective") String objective, @Param("since") LocalDateTime since);
}
