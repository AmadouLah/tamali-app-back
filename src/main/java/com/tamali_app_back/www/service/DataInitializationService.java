package com.tamali_app_back.www.service;

import com.tamali_app_back.www.entity.Business;
import com.tamali_app_back.www.entity.Role;
import com.tamali_app_back.www.entity.User;
import com.tamali_app_back.www.enums.RoleType;
import com.tamali_app_back.www.repository.BusinessRepository;
import com.tamali_app_back.www.repository.RoleRepository;
import com.tamali_app_back.www.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializationService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BusinessRepository businessRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            log.info("Début de l'initialisation des données par défaut...");
            Thread.sleep(500);
            ensureRolesExist();
            ensureDefaultBusinessExists();
            initializeDefaultUsers();
            log.info("Initialisation des données par défaut terminée.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Initialisation interrompue", e);
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation des données par défaut", e);
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

    private Business ensureDefaultBusinessExists() {
        List<Business> all = businessRepository.findAll();
        if (all.isEmpty()) {
            Business b = Business.builder()
                    .name("Tamali Demo")
                    .email("demo@tamali.ml")
                    .phone("+22300000000")
                    .address("Bamako, Mali")
                    .active(true)
                    .build();
            businessRepository.save(b);
            log.info("Business par défaut créé: Tamali Demo");
            return b;
        }
        return all.get(0);
    }

    private void initializeDefaultUsers() {
        Business defaultBusiness = businessRepository.findAll().stream().findFirst().orElse(null);
        List<DefaultUser> defaults = Arrays.asList(
                new DefaultUser(
                        "amadoulandoure004@gmail.com",
                        "Admin@2024!",
                        "Admin",
                        "Amadou",
                        RoleType.ADMIN,
                        null),
                new DefaultUser(
                        "amadoulandoure89@gmail.com",
                        "Owner@2024!",
                        "Propriétaire",
                        "Demo",
                        RoleType.OWNER,
                        defaultBusiness),
                new DefaultUser(
                        "landoureamadou89@gmail.com",
                        "Manager@2024!",
                        "Manager",
                        "Demo",
                        RoleType.MANAGER,
                        defaultBusiness),
                new DefaultUser(
                        "cashier@tamali.ml",
                        "Cashier@2024!",
                        "Caissier",
                        "Demo",
                        RoleType.CASHIER,
                        defaultBusiness));

        int created = 0;
        int updated = 0;

        for (DefaultUser du : defaults) {
            try {
                String email = du.email().trim();
                var existingOpt = userRepository.findByEmailIgnoreCase(email);

                if (existingOpt.isEmpty()) {
                    User user = buildUser(du);
                    userRepository.save(user);
                    userRepository.flush();
                    created++;
                    log.info("Utilisateur par défaut créé: email={}, role={}", email, du.roleType());
                    continue;
                }

                User existing = existingOpt.get();
                boolean needUpdate = false;
                if (!passwordEncoder.matches(du.rawPassword(), existing.getPassword())) {
                    existing.setPassword(passwordEncoder.encode(du.rawPassword()));
                    needUpdate = true;
                }
                if (!existing.isEnabled()) {
                    existing.setEnabled(true);
                    needUpdate = true;
                }
                Role role = roleRepository.findByType(du.roleType()).orElse(null);
                if (role != null && (existing.getRoles() == null || !existing.getRoles().stream().anyMatch(r -> r.getType() == du.roleType()))) {
                    existing.setRoles(Set.of(role));
                    needUpdate = true;
                }
                if (du.business() != null && (existing.getBusiness() == null || !existing.getBusiness().getId().equals(du.business().getId()))) {
                    existing.setBusiness(du.business());
                    needUpdate = true;
                }

                if (needUpdate) {
                    userRepository.save(existing);
                    userRepository.flush();
                    updated++;
                    log.info("Utilisateur par défaut mis à jour: email={}, role={}", email, du.roleType());
                }
            } catch (Exception e) {
                log.error("Erreur lors de l'initialisation de l'utilisateur {}: {}", du.email(), e.getMessage());
            }
        }

        if (created > 0 || updated > 0) {
            log.info("Synthèse init users -> créés: {}, mis à jour: {}", created, updated);
        } else {
            log.info("Utilisateurs par défaut déjà conformes.");
        }
    }

    private User buildUser(DefaultUser du) {
        Role role = roleRepository.findByType(du.roleType()).orElseThrow();
        return User.builder()
                .firstname(du.firstname())
                .lastname(du.lastname())
                .email(du.email())
                .password(passwordEncoder.encode(du.rawPassword()))
                .enabled(true)
                .business(du.business())
                .roles(Set.of(role))
                .verificationCode(null)
                .codeExpiration(null)
                .build();
    }

    private record DefaultUser(
            String email,
            String rawPassword,
            String firstname,
            String lastname,
            RoleType roleType,
            Business business) {
    }
}
