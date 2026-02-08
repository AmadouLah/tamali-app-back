package com.tamali_app_back.www.repository;

import com.tamali_app_back.www.entity.Role;
import com.tamali_app_back.www.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByType(RoleType type);
}
