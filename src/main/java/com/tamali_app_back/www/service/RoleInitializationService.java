package com.tamali_app_back.www.service;

import com.tamali_app_back.www.enums.RoleType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class RoleInitializationService implements CommandLineRunner {

    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(String... args) {
        try {
            // Supprimer d'abord les références dans user_roles, puis les rôles invalides
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    // D'abord supprimer les références dans user_roles pour les rôles invalides
                    // IMPORTANT: Ne PAS supprimer BUSINESS_ASSOCIATE qui est un rôle valide !
                    Query deleteUserRolesQuery = entityManager.createNativeQuery(
                        "DELETE FROM user_roles ur " +
                        "USING roles r " +
                        "WHERE ur.role_id = r.id " +
                        "AND r.name NOT IN ('SUPER_ADMIN', 'BUSINESS_OWNER', 'BUSINESS_ASSOCIATE')"
                    );
                    int deletedUserRoles = deleteUserRolesQuery.executeUpdate();
                    if (deletedUserRoles > 0) {
                        log.info("Références de rôles invalides supprimées de user_roles: {}", deletedUserRoles);
                    }
                    
                    // Ensuite supprimer les rôles invalides
                    // IMPORTANT: Ne PAS supprimer BUSINESS_ASSOCIATE qui est un rôle valide !
                    Query deleteRolesQuery = entityManager.createNativeQuery(
                        "DELETE FROM roles WHERE name NOT IN ('SUPER_ADMIN', 'BUSINESS_OWNER', 'BUSINESS_ASSOCIATE')"
                    );
                    int deletedRoles = deleteRolesQuery.executeUpdate();
                    if (deletedRoles > 0) {
                        log.info("Rôles invalides supprimés: {}", deletedRoles);
                    }
                } catch (Exception e) {
                    log.warn("Erreur lors de la suppression des rôles invalides: {}", e.getMessage());
                    status.setRollbackOnly();
                }
            });
        } catch (Exception e) {
            log.warn("Erreur lors de la suppression des rôles invalides (transaction annulée): {}", e.getMessage());
        }
        
        // Supprimer ou modifier la contrainte CHECK si elle existe
        try {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    Query dropConstraintQuery = entityManager.createNativeQuery(
                        "ALTER TABLE roles DROP CONSTRAINT IF EXISTS roles_name_check"
                    );
                    dropConstraintQuery.executeUpdate();
                    log.info("Contrainte CHECK sur roles supprimée si elle existait");
                } catch (Exception e) {
                    log.debug("Contrainte CHECK déjà absente ou erreur lors de la suppression: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.debug("Erreur lors de la suppression de la contrainte CHECK: {}", e.getMessage());
        }

        // Créer chaque rôle dans sa propre transaction pour éviter les rollbacks en cascade
        for (RoleType rt : RoleType.values()) {
            try {
                transactionTemplate.executeWithoutResult(status -> ensureRoleExists(rt));
            } catch (Exception e) {
                log.warn("Erreur lors de l'initialisation du rôle {}: {}", rt, e.getMessage());
            }
        }
    }

    private void ensureRoleExists(RoleType rt) {
        try {
            // Vérifier d'abord via requête native si le rôle existe
            Query checkQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM roles WHERE name = :name AND deleted_at IS NULL"
            );
            checkQuery.setParameter("name", rt.name());
            Long count = ((Number) checkQuery.getSingleResult()).longValue();
            
            if (count == 0) {
                // Le rôle n'existe pas, le créer
                Query insertQuery = entityManager.createNativeQuery(
                    "INSERT INTO roles (id, name, created_at, updated_at, version, deleted_at) " +
                    "VALUES (gen_random_uuid(), :name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, NULL) " +
                    "ON CONFLICT (name) DO NOTHING"
                );
                insertQuery.setParameter("name", rt.name());
                int inserted = insertQuery.executeUpdate();
                if (inserted > 0) {
                    log.info("Rôle créé: {}", rt);
                } else {
                    log.debug("Rôle {} existe déjà (conflit géré)", rt);
                }
            } else {
                log.debug("Rôle {} existe déjà", rt);
            }
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification/création du rôle {}: {}", rt, e.getMessage());
            throw e; // Propager l'erreur pour marquer la transaction comme rollback-only
        }
    }
}
