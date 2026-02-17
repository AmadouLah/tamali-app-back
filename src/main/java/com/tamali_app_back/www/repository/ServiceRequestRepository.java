package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByProcessedFalseOrderByCreatedAtDesc();

    List<ServiceRequest> findAllByOrderByCreatedAtDesc();
}
