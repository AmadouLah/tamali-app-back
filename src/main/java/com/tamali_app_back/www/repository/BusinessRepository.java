package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {}
