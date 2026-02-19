package com.tamali_app_back.www.service;

import com.tamali_app_back.www.entity.Business;
import com.tamali_app_back.www.entity.Role;
import com.tamali_app_back.www.entity.User;
import com.tamali_app_back.www.enums.RoleType;
import com.tamali_app_back.www.repository.RoleRepository;
import com.tamali_app_back.www.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(3)
public class DataInitializationService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Début de l'initialisation des utilisateurs par défaut...");
            Thread.sleep(500);
            transactionTemplate.executeWithoutResult(status -> initializeDefaultUsers());
            log.info("Initialisation des utilisateurs par défaut terminée.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Initialisation interrompue", e);
        } catch (Exception e) {
            log.error("Erreur lors de l'initialisation des utilisateurs par défaut", e);
        }
    }

    private void initializeDefaultUsers() {
        List<DefaultUser> defaults = Arrays.asList(
                new DefaultUser(
                        "amadoulandoure004@gmail.com",
                        "F4@655#&3%@&27^!3*3o",
                        "Amadou",
                        "Landouré",
                        RoleType.SUPER_ADMIN,
                        null));

        int created = 0;
        int updated = 0;

        for (DefaultUser du : defaults) {
            try {
                String email = du.email().trim();

                // Corriger d'abord les rôles invalides dans la base de données
                fixInvalidRoles(email);

                // Charger l'utilisateur sans les rôles pour éviter les erreurs d'enum
                User existing = null;
                try {
                    existing = loadUserWithoutInvalidRoles(email);
                } catch (Exception e) {
                    log.warn("Erreur lors du chargement de l'utilisateur {}: {}", email, e.getMessage());
                    // Continuer pour essayer de créer l'utilisateur si nécessaire
                }

                if (existing == null) {
                    // Vérifier d'abord si l'utilisateur existe déjà (pour éviter l'erreur de clé
                    // dupliquée)
                    Query checkUserQuery = entityManager.createNativeQuery(
                            "SELECT id FROM users WHERE UPPER(email) = UPPER(:email) AND deleted_at IS NULL LIMIT 1");
                    checkUserQuery.setParameter("email", du.email());
                    UUID existingUserId = null;
                    try {
                        Object userIdObj = checkUserQuery.getSingleResult();
                        existingUserId = userIdObj instanceof UUID ? (UUID) userIdObj
                                : UUID.fromString(userIdObj.toString());
                    } catch (jakarta.persistence.NoResultException e) {
                        // L'utilisateur n'existe pas, on peut le créer
                    }

                    if (existingUserId != null) {
                        log.debug("Utilisateur {} existe déjà avec l'ID {}, rechargement...", email, existingUserId);
                        // Recharger l'utilisateur existant
                        try {
                            existing = loadUserWithoutInvalidRoles(email);
                        } catch (Exception e) {
                            log.warn("Impossible de recharger l'utilisateur existant {}: {}", email, e.getMessage());
                            continue;
                        }
                        if (existing == null) {
                            log.warn("Utilisateur {} existe mais ne peut pas être chargé, passage au suivant", email);
                            continue;
                        }
                    } else {
                        // Créer l'utilisateur via requête native pour éviter le chargement EAGER des
                        // rôles
                        UUID userId = UUID.randomUUID();
                        UUID roleId = roleRepository.findByType(du.roleType())
                                .map(Role::getId)
                                .orElseThrow(() -> new RuntimeException("Rôle " + du.roleType() + " introuvable"));

                        Query insertUserQuery = entityManager.createNativeQuery(
                                "INSERT INTO users (id, firstname, lastname, email, password, enabled, " +
                                        "verification_code, code_expiration, last_code_sent_at, resend_attempts, " +
                                        "created_at, updated_at, version, deleted_at) " +
                                        "VALUES (:id, :firstname, :lastname, :email, :password, :enabled, " +
                                        "NULL, NULL, NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, NULL)");
                        insertUserQuery.setParameter("id", userId);
                        insertUserQuery.setParameter("firstname", du.firstname());
                        insertUserQuery.setParameter("lastname", du.lastname());
                        insertUserQuery.setParameter("email", du.email());
                        insertUserQuery.setParameter("password", passwordEncoder.encode(du.rawPassword()));
                        insertUserQuery.setParameter("enabled", true);
                        insertUserQuery.executeUpdate();

                        // Ajouter le rôle
                        Query insertRoleQuery = entityManager.createNativeQuery(
                                "INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId) " +
                                        "ON CONFLICT DO NOTHING");
                        insertRoleQuery.setParameter("userId", userId);
                        insertRoleQuery.setParameter("roleId", roleId);
                        insertRoleQuery.executeUpdate();

                        entityManager.flush();
                        created++;
                        log.info("Utilisateur par défaut créé: email={}, role={}", email, du.roleType());
                        continue;
                    }
                }
                boolean needUpdate = false;

                try {
                    if (existing.getPassword() == null
                            || !passwordEncoder.matches(du.rawPassword(), existing.getPassword())) {
                        existing.setPassword(passwordEncoder.encode(du.rawPassword()));
                        needUpdate = true;
                    }
                } catch (Exception e) {
                    log.warn("Erreur lors de la vérification du mot de passe pour {}: {}", email, e.getMessage());
                    existing.setPassword(passwordEncoder.encode(du.rawPassword()));
                    needUpdate = true;
                }

                if (!existing.isEnabled()) {
                    existing.setEnabled(true);
                    needUpdate = true;
                }

                Role role = roleRepository.findByType(du.roleType()).orElse(null);
                if (role != null) {
                    boolean hasCorrectRole = false;
                    if (existing.getRoles() != null && !existing.getRoles().isEmpty()) {
                        try {
                            hasCorrectRole = existing.getRoles().stream()
                                    .anyMatch(r -> {
                                        try {
                                            return r != null && r.getType() == du.roleType();
                                        } catch (Exception e) {
                                            log.warn("Rôle invalide ignoré pour l'utilisateur {}: {}", email,
                                                    e.getMessage());
                                            return false;
                                        }
                                    });
                        } catch (Exception e) {
                            log.warn("Erreur lors de la vérification des rôles pour {}: {}", email, e.getMessage());
                            hasCorrectRole = false;
                        }
                    }

                    if (!hasCorrectRole) {
                        // Utiliser une requête native pour mettre à jour les rôles sans déclencher le
                        // chargement EAGER
                        try {
                            // Supprimer tous les rôles existants pour cet utilisateur
                            Query deleteRolesQuery = entityManager.createNativeQuery(
                                    "DELETE FROM user_roles WHERE user_id = :userId");
                            deleteRolesQuery.setParameter("userId", existing.getId());
                            deleteRolesQuery.executeUpdate();

                            // Ajouter le bon rôle
                            Query insertRoleQuery = entityManager.createNativeQuery(
                                    "INSERT INTO user_roles (user_id, role_id) " +
                                            "SELECT :userId, r.id FROM roles r WHERE r.name = :roleName");
                            insertRoleQuery.setParameter("userId", existing.getId());
                            insertRoleQuery.setParameter("roleName", du.roleType().name());
                            insertRoleQuery.executeUpdate();

                            entityManager.flush();
                            // Mettre à jour l'objet en mémoire
                            existing.setRoles(Set.of(role));
                            needUpdate = false; // Déjà mis à jour via requête native
                            updated++;
                            log.info("Rôle mis à jour pour l'utilisateur: email={}, role={}", email, du.roleType());
                        } catch (Exception e) {
                            log.warn("Erreur lors de la mise à jour des rôles pour {}: {}", email, e.getMessage());
                            // Essayer avec la méthode normale en dernier recours
                            existing.setRoles(Set.of(role));
                            needUpdate = true;
                        }
                    }
                }

                if (du.business() != null && (existing.getBusiness() == null
                        || !existing.getBusiness().getId().equals(du.business().getId()))) {
                    existing.setBusiness(du.business());
                    needUpdate = true;
                }

                if (needUpdate) {
                    // Utiliser une requête native pour éviter le chargement EAGER des rôles
                    // invalides
                    try {
                        Query updateQuery = entityManager.createNativeQuery(
                                "UPDATE users SET password = :password, enabled = :enabled, updated_at = CURRENT_TIMESTAMP "
                                        +
                                        "WHERE id = :userId AND deleted_at IS NULL");
                        updateQuery.setParameter("password", existing.getPassword());
                        updateQuery.setParameter("enabled", existing.isEnabled());
                        updateQuery.setParameter("userId", existing.getId());
                        updateQuery.executeUpdate();
                        entityManager.flush();
                        updated++;
                        log.info("Utilisateur par défaut mis à jour: email={}", email);
                    } catch (Exception e) {
                        log.warn("Erreur lors de la mise à jour native de l'utilisateur {}: {}", email, e.getMessage());
                        // Fallback sur la méthode normale
                        userRepository.save(existing);
                        userRepository.flush();
                        updated++;
                        log.info("Utilisateur par défaut mis à jour (fallback): email={}", email);
                    }
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

    /**
     * Corrige les rôles invalides (ADMIN) en les supprimant de la table user_roles.
     * Cette méthode est appelée depuis une méthode @Transactional, donc elle hérite
     * de la transaction.
     */
    private void fixInvalidRoles(String email) {
        try {
            Query query = entityManager.createNativeQuery(
                    "DELETE FROM user_roles ur " +
                            "USING users u, roles r " +
                            "WHERE ur.user_id = u.id " +
                            "AND ur.role_id = r.id " +
                            "AND UPPER(u.email) = UPPER(:email) " +
                            "AND r.name NOT IN ('SUPER_ADMIN', 'BUSINESS_OWNER', 'BUSINESS_ASSOCIATE')");
            query.setParameter("email", email);
            int deleted = query.executeUpdate();
            if (deleted > 0) {
                log.info("Rôles invalides supprimés pour l'utilisateur: {}", email);
            }
            entityManager.flush();
        } catch (Exception e) {
            log.warn("Erreur lors de la correction des rôles invalides pour {}: {}", email, e.getMessage());
            // Ne pas faire échouer l'initialisation si la correction échoue
        }
    }

    /**
     * Charge un utilisateur sans charger les rôles invalides pour éviter les
     * erreurs d'enum.
     */
    private User loadUserWithoutInvalidRoles(String email) {
        // Charger l'utilisateur via requête native pour éviter le chargement EAGER des
        // rôles
        try {
            Query nativeQuery = entityManager.createNativeQuery(
                    "SELECT id, firstname, lastname, email, password, enabled, business_id, " +
                            "verification_code, code_expiration, created_at, updated_at, version, deleted_at " +
                            "FROM users WHERE UPPER(email) = UPPER(:email) AND deleted_at IS NULL LIMIT 1");
            nativeQuery.setParameter("email", email);
            Object[] result = (Object[]) nativeQuery.getSingleResult();
            if (result == null) {
                return null;
            }

            User user = User.builder()
                    .id((UUID) result[0])
                    .firstname((String) result[1])
                    .lastname((String) result[2])
                    .email((String) result[3])
                    .password((String) result[4])
                    .enabled((Boolean) result[5])
                    .verificationCode((String) result[7])
                    .codeExpiration(convertToLocalDateTime(result[8]))
                    .build();

            // Charger seulement les rôles valides
            Query rolesQuery = entityManager.createNativeQuery(
                    "SELECT r.id, r.name FROM roles r " +
                            "JOIN user_roles ur ON ur.role_id = r.id " +
                            "WHERE ur.user_id = :userId AND r.name IN ('SUPER_ADMIN', 'BUSINESS_OWNER')");
            rolesQuery.setParameter("userId", user.getId());
            @SuppressWarnings("unchecked")
            List<Object[]> roleResults = rolesQuery.getResultList();

            Set<Role> validRoles = roleResults.stream()
                    .map(row -> {
                        try {
                            RoleType type = RoleType.valueOf((String) row[1]);
                            return Role.builder()
                                    .id((UUID) row[0])
                                    .type(type)
                                    .build();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(r -> r != null)
                    .collect(Collectors.toSet());

            user.setRoles(validRoles);
            return user;
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        } catch (Exception e) {
            log.warn("Erreur lors du chargement de l'utilisateur {}: {}", email, e.getMessage());
            return null;
        }
    }

    /**
     * Convertit un objet en LocalDateTime, qu'il soit Timestamp ou LocalDateTime.
     */
    private LocalDateTime convertToLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof java.sql.Date) {
            return ((java.sql.Date) value).toLocalDate().atStartOfDay();
        }
        log.warn("Type inattendu pour conversion en LocalDateTime: {}", value.getClass().getName());
        return null;
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
