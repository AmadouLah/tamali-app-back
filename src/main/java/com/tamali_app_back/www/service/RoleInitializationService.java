package com.tamali_app_back.www.service;

import com.tamali_app_back.www.entity.Role;
import com.tamali_app_back.www.enums.RoleType;
import com.tamali_app_back.www.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class RoleInitializationService implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        try {
            ensureRolesExist();
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation des rôles", e);
        }
    }

    private void ensureRolesExist() {
        for (RoleType rt : RoleType.values()) {
            if (roleRepository.findByType(rt).isEmpty()) {
                Role role = Role.builder().type(rt).build();
                roleRepository.save(role);
                log.info("Rôle créé: {}", rt);
            }
        }
    }
}
