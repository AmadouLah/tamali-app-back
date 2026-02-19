package com.tamali_app_back.www.service;

import com.tamali_app_back.www.dto.EmailCheckResponse;
import com.tamali_app_back.www.dto.UserDto;
import com.tamali_app_back.www.entity.Business;
import com.tamali_app_back.www.entity.Role;
import com.tamali_app_back.www.entity.User;
import com.tamali_app_back.www.enums.RoleType;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.exception.ResourceNotFoundException;
import com.tamali_app_back.www.repository.BusinessRepository;
import com.tamali_app_back.www.repository.RoleRepository;
import com.tamali_app_back.www.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private static final int CODE_VALIDITY_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 30;
    private static final int MAX_RESEND_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final RoleRepository roleRepository;
    private final MailService mailService;
    private final EntityMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final BusinessService businessService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Transactional(readOnly = true)
    public UserDto findByEmail(String email) {
        return userRepository.findByEmail(email).map(mapper::toDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public UserDto getById(UUID id) {
        return userRepository.findById(id).map(mapper::toDto).orElse(null);
    }

    /**
     * Récupère tous les utilisateurs avec le rôle BUSINESS_OWNER.
     */
    @Transactional(readOnly = true)
    public List<UserDto> findAllBusinessOwners() {
        Role businessOwnerRole = roleRepository.findByType(RoleType.BUSINESS_OWNER)
                .orElseThrow(() -> new IllegalStateException("Rôle BUSINESS_OWNER introuvable."));
        
        Query nativeQuery = entityManager.createNativeQuery(
            "SELECT DISTINCT u.id, u.firstname, u.lastname, u.email, u.enabled, u.business_id, " +
            "u.must_change_password, u.created_at, u.updated_at, u.version, u.deleted_at " +
            "FROM users u " +
            "JOIN user_roles ur ON ur.user_id = u.id " +
            "WHERE ur.role_id = :roleId AND u.deleted_at IS NULL " +
            "ORDER BY u.created_at DESC"
        );
        nativeQuery.setParameter("roleId", businessOwnerRole.getId());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();
        
        return results.stream()
                .map(result -> {
                    UUID userId = (UUID) result[0];
                    UUID ownerBusinessId = result[5] != null ? (UUID) result[5] : null;
                    Set<Role> roles = loadValidRoles(userId);
                    Business business = ownerBusinessId != null ? businessRepository.findById(ownerBusinessId).orElse(null) : null;
                    User user = User.builder()
                            .id(userId)
                            .firstname((String) result[1])
                            .lastname((String) result[2])
                            .email((String) result[3])
                            .enabled((Boolean) result[4])
                            .business(business)
                            .mustChangePassword(result[6] != null && ((Boolean) result[6]))
                            .build();
                    user.setRoles(roles);
                    return mapper.toDto(user);
                })
                .collect(Collectors.toList());
    }

    /**
     * Récupère tous les associés d'une entreprise (utilisateurs avec le rôle BUSINESS_ASSOCIATE).
     */
    @Transactional(readOnly = true)
    public List<UserDto> findAssociatesByBusinessId(UUID businessId) {
        Role associateRole = roleRepository.findByType(RoleType.BUSINESS_ASSOCIATE)
                .orElseThrow(() -> new IllegalStateException("Rôle BUSINESS_ASSOCIATE introuvable."));
        
        log.info("Recherche des associés pour l'entreprise: {} avec le rôle ID: {} (nom: BUSINESS_ASSOCIATE)", 
                businessId, associateRole.getId());
        
        // Diagnostic: Vérifier combien d'utilisateurs ont le business_id
        Query diagnosticQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM users WHERE business_id = :businessId AND deleted_at IS NULL"
        );
        diagnosticQuery.setParameter("businessId", businessId);
        Long totalUsersWithBusiness = ((Number) diagnosticQuery.getSingleResult()).longValue();
        log.info("Diagnostic: {} utilisateurs trouvés avec business_id = {}", totalUsersWithBusiness, businessId);
        
        // Diagnostic: Vérifier combien d'utilisateurs ont le rôle BUSINESS_ASSOCIATE
        Query diagnosticRoleQuery = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM users u " +
            "JOIN user_roles ur ON ur.user_id = u.id " +
            "JOIN roles r ON ur.role_id = r.id " +
            "WHERE r.name = 'BUSINESS_ASSOCIATE' AND u.deleted_at IS NULL"
        );
        Long totalAssociates = ((Number) diagnosticRoleQuery.getSingleResult()).longValue();
        log.info("Diagnostic: {} utilisateurs au total ont le rôle BUSINESS_ASSOCIATE dans toute la base", totalAssociates);
        
        // Diagnostic: Vérifier les utilisateurs avec ce business_id et leurs rôles
        Query diagnosticBusinessRoleQuery = entityManager.createNativeQuery(
            "SELECT u.id, u.email, r.name " +
            "FROM users u " +
            "LEFT JOIN user_roles ur ON ur.user_id = u.id " +
            "LEFT JOIN roles r ON ur.role_id = r.id " +
            "WHERE u.business_id = :businessId AND u.deleted_at IS NULL"
        );
        diagnosticBusinessRoleQuery.setParameter("businessId", businessId);
        @SuppressWarnings("unchecked")
        List<Object[]> diagnosticResults = diagnosticBusinessRoleQuery.getResultList();
        log.info("Diagnostic: Détails des utilisateurs avec business_id = {}:", businessId);
        for (Object[] row : diagnosticResults) {
            log.info("  - Utilisateur ID: {}, Email: {}, Rôle: {}", row[0], row[1], row[2]);
        }
        
        // Vérifier et restaurer les rôles manquants pour les utilisateurs qui devraient être des associés
        restoreMissingAssociateRoles(businessId, associateRole.getId());
        
        // Utiliser le nom du rôle directement dans la requête pour plus de robustesse
        Query nativeQuery = entityManager.createNativeQuery(
            "SELECT DISTINCT u.id, u.firstname, u.lastname, u.email, u.enabled, u.business_id, " +
            "u.must_change_password, u.created_at, u.updated_at, u.version, u.deleted_at " +
            "FROM users u " +
            "JOIN user_roles ur ON ur.user_id = u.id " +
            "JOIN roles r ON ur.role_id = r.id " +
            "WHERE r.name = 'BUSINESS_ASSOCIATE' AND u.business_id = :businessId AND u.deleted_at IS NULL " +
            "ORDER BY u.created_at DESC"
        );
        nativeQuery.setParameter("businessId", businessId);
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();
        
        log.info("Trouvé {} associés pour l'entreprise {}", results.size(), businessId);
        
        return results.stream()
                .map(result -> {
                    UUID userId = (UUID) result[0];
                    UUID ownerBusinessId = result[5] != null ? (UUID) result[5] : null;
                    Set<Role> roles = loadValidRoles(userId);
                    Business business = ownerBusinessId != null ? businessRepository.findById(ownerBusinessId).orElse(null) : null;
                    User user = User.builder()
                            .id(userId)
                            .firstname((String) result[1])
                            .lastname((String) result[2])
                            .email((String) result[3])
                            .enabled((Boolean) result[4])
                            .business(business)
                            .mustChangePassword(result[6] != null && ((Boolean) result[6]))
                            .build();
                    user.setRoles(roles);
                    log.info("Associé trouvé: {} (ID: {}) avec {} rôles", result[3], userId, roles.size());
                    return mapper.toDto(user);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto create(String firstname, String lastname, String email, String password, UUID businessId, Set<RoleType> roleTypes) {
        String trimmedEmail = email.trim();
        
        // Supprimer définitivement les utilisateurs supprimés avec cet email pour permettre la réutilisation
        deleteSoftDeletedUserByEmail(trimmedEmail);
        
        if (userRepository.existsByEmail(trimmedEmail)) return null;
        
        Business business = businessId != null ? businessRepository.findById(businessId).orElse(null) : null;
        Set<Role> roles = roleTypes != null && !roleTypes.isEmpty()
                ? roleTypes.stream()
                .flatMap(rt -> roleRepository.findByType(rt).stream())
                .collect(java.util.stream.Collectors.toSet())
                : Set.of();
        User u = User.builder()
                .firstname(firstname)
                .lastname(lastname)
                .email(trimmedEmail)
                .password(passwordEncoder.encode(password))
                .enabled(false)
                .business(business)
                .roles(roles)
                .build();
        return mapper.toDto(userRepository.save(u));
    }

    /**
     * Crée un propriétaire d'entreprise avec juste l'email.
     * Génère un mot de passe temporaire de 10 caractères et l'envoie par email.
     * Si l'utilisateur existe déjà mais n'a pas encore changé son mot de passe temporaire, génère un nouveau mot de passe.
     * Si une violation de contrainte se produit (utilisateur existant), retourne l'utilisateur existant au lieu de lever une erreur.
     */
    @Transactional(noRollbackFor = org.springframework.dao.DataIntegrityViolationException.class)
    public UserDto createBusinessOwnerWithEmail(String email) {
        entityManager.clear();
        entityManager.flush(); // S'assurer que toutes les modifications précédentes sont flushées
        
        String trimmedEmail = email.trim();
        log.debug("Tentative de création de propriétaire d'entreprise avec email: {}", trimmedEmail);
        
        // Supprimer définitivement les utilisateurs supprimés avec cet email pour permettre la réutilisation
        deleteSoftDeletedUserByEmail(trimmedEmail);
        
        // Vérifier maintenant si un utilisateur actif existe avec cet email
        // Utiliser une requête native pour éviter les problèmes de cache Hibernate
        UUID foundUserId = null;
        try {
            Query checkUserQuery = entityManager.createNativeQuery(
                "SELECT id FROM users WHERE UPPER(email) = UPPER(:email) AND deleted_at IS NULL LIMIT 1"
            );
            checkUserQuery.setParameter("email", trimmedEmail);
            // Forcer l'exécution immédiate de la requête
            @SuppressWarnings("unchecked")
            List<Object> results = checkUserQuery.getResultList();
            if (!results.isEmpty() && results.get(0) != null) {
                foundUserId = (UUID) results.get(0);
                log.debug("Utilisateur existant trouvé avec l'email: {} (ID: {})", trimmedEmail, foundUserId);
            }
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification de l'email {}: {}", trimmedEmail, e.getMessage());
            // En cas d'erreur, on continue pour éviter de bloquer la création
        }
        
        // Si un utilisateur existe, le charger et gérer selon son état
        if (foundUserId != null) {
            final UUID existingUserId = foundUserId; // Variable finale pour utilisation dans le bloc
            User existingUser = loadUserByIdWithoutInvalidRoles(existingUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", existingUserId));
            // Si l'utilisateur existe déjà et doit changer son mot de passe temporaire, régénérer le mot de passe
            if (existingUser.isMustChangePassword()) {
                log.info("Utilisateur {} existe déjà avec mustChangePassword=true, régénération du mot de passe temporaire", trimmedEmail);
                
                String temporaryPassword = generateTemporaryPassword();
                String encodedPassword = passwordEncoder.encode(temporaryPassword);
                
                // Mettre à jour le mot de passe via requête native
                Query updateQuery = entityManager.createNativeQuery(
                    "UPDATE users SET password = :password, updated_at = :updatedAt " +
                    "WHERE id = :userId AND deleted_at IS NULL"
                );
                updateQuery.setParameter("password", encodedPassword);
                updateQuery.setParameter("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
                updateQuery.setParameter("userId", existingUser.getId());
                int updated = updateQuery.executeUpdate();
                
                if (updated == 0) {
                    throw new ResourceNotFoundException("Utilisateur", existingUser.getId());
                }
                
                // Envoyer l'email avec le mot de passe temporaire (après le commit de la transaction)
                sendTemporaryPasswordEmail(trimmedEmail, temporaryPassword);
                
                log.info("Mot de passe temporaire régénéré pour l'utilisateur: {}", trimmedEmail);
                return mapper.toDto(existingUser);
            } else {
                // L'utilisateur existe et a déjà changé son mot de passe
                // Vérifier s'il a déjà le rôle BUSINESS_OWNER
                boolean hasBusinessOwnerRole = existingUser.getRoles().stream()
                        .anyMatch(role -> role.getType() == RoleType.BUSINESS_OWNER);
                
                if (hasBusinessOwnerRole) {
                    // L'utilisateur a déjà le rôle BUSINESS_OWNER, retourner l'utilisateur existant (idempotent)
                    log.info("Utilisateur {} existe déjà avec le rôle BUSINESS_OWNER, retour de l'utilisateur existant", trimmedEmail);
                    return mapper.toDto(existingUser);
                } else {
                    // L'utilisateur existe mais n'a pas le rôle BUSINESS_OWNER, lui ajouter ce rôle
                    // et générer un nouveau mot de passe temporaire pour qu'il puisse se connecter
                    log.info("Utilisateur {} existe déjà sans le rôle BUSINESS_OWNER, ajout du rôle et génération d'un mot de passe temporaire", trimmedEmail);
                    
                    Role businessOwnerRole = roleRepository.findByType(RoleType.BUSINESS_OWNER)
                            .orElseThrow(() -> new IllegalStateException("Rôle BUSINESS_OWNER introuvable."));
                    
                    // Générer un nouveau mot de passe temporaire
                    String temporaryPassword = generateTemporaryPassword();
                    String encodedPassword = passwordEncoder.encode(temporaryPassword);
                    
                    // Ajouter le rôle et mettre à jour le mot de passe via requête native
                    Query insertRoleQuery = entityManager.createNativeQuery(
                        "INSERT INTO user_roles (user_id, role_id) " +
                        "SELECT :userId, :roleId " +
                        "WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = :userId AND role_id = :roleId)"
                    );
                    insertRoleQuery.setParameter("userId", existingUser.getId());
                    insertRoleQuery.setParameter("roleId", businessOwnerRole.getId());
                    insertRoleQuery.executeUpdate();
                    
                    // Mettre à jour le mot de passe et mustChangePassword
                    Query updatePasswordQuery = entityManager.createNativeQuery(
                        "UPDATE users SET password = :password, must_change_password = true, updated_at = :updatedAt " +
                        "WHERE id = :userId AND deleted_at IS NULL"
                    );
                    updatePasswordQuery.setParameter("password", encodedPassword);
                    updatePasswordQuery.setParameter("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
                    updatePasswordQuery.setParameter("userId", existingUser.getId());
                    int updated = updatePasswordQuery.executeUpdate();
                    
                    if (updated == 0) {
                        throw new ResourceNotFoundException("Utilisateur", existingUser.getId());
                    }
                    
                    // Envoyer l'email avec le mot de passe temporaire (après le commit de la transaction)
                    sendTemporaryPasswordEmail(trimmedEmail, temporaryPassword);
                    
                    // Recharger l'utilisateur avec les nouveaux rôles
                    entityManager.clear();
                    User updatedUser = loadUserWithoutInvalidRoles(trimmedEmail);
                    
                    log.info("Rôle BUSINESS_OWNER ajouté et mot de passe temporaire envoyé à l'utilisateur: {}", trimmedEmail);
                    return mapper.toDto(updatedUser);
                }
            }
        }

        // Vérifier une dernière fois avant de créer pour éviter les doublons (race condition)
        try {
            Query finalCheckQuery = entityManager.createNativeQuery(
                "SELECT id FROM users WHERE UPPER(email) = UPPER(:email) AND deleted_at IS NULL LIMIT 1"
            );
            finalCheckQuery.setParameter("email", trimmedEmail);
            @SuppressWarnings("unchecked")
            List<Object> finalResults = finalCheckQuery.getResultList();
            if (!finalResults.isEmpty() && finalResults.get(0) != null) {
                UUID finalCheck = (UUID) finalResults.get(0);
                // L'utilisateur a été créé entre-temps, le charger et retourner
                User raceConditionUser = loadUserByIdWithoutInvalidRoles(finalCheck)
                        .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", finalCheck));
                log.info("Utilisateur {} créé entre-temps, retour de l'utilisateur existant", trimmedEmail);
                return mapper.toDto(raceConditionUser);
            }
        } catch (Exception e) {
            log.debug("Vérification finale avant création: aucun utilisateur trouvé pour {}", trimmedEmail);
            // OK, l'utilisateur n'existe toujours pas, on peut créer
        }

        // Créer un nouvel utilisateur
        Role businessOwnerRole = roleRepository.findByType(RoleType.BUSINESS_OWNER)
                .orElseThrow(() -> new IllegalStateException("Rôle BUSINESS_OWNER introuvable."));

        String temporaryPassword = generateTemporaryPassword();
        String encodedPassword = passwordEncoder.encode(temporaryPassword);

        User user = User.builder()
                .email(trimmedEmail)
                .password(encodedPassword)
                .enabled(true)
                .mustChangePassword(true)
                .business(null)
                .roles(Set.of(businessOwnerRole))
                .build();

        User savedUser;
        try {
            savedUser = userRepository.save(user);
            entityManager.flush(); // Forcer le flush pour détecter immédiatement les violations de contrainte
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // Violation de contrainte détectée - vérifier si c'est un utilisateur supprimé ou actif
            log.warn("Violation de contrainte détectée pour {}, vérification de l'utilisateur existant", trimmedEmail);
            entityManager.clear();
            
            // Vérifier si un utilisateur supprimé existe et le supprimer définitivement
            if (deleteSoftDeletedUserByEmail(trimmedEmail)) {
                // Réessayer de créer l'utilisateur après la suppression
                try {
                    savedUser = userRepository.save(user);
                    entityManager.flush();
                    log.info("Utilisateur créé avec succès après suppression de l'utilisateur supprimé: {}", trimmedEmail);
                    // Envoyer l'email avec le mot de passe temporaire (après le commit de la transaction)
                    sendTemporaryPasswordEmail(trimmedEmail, temporaryPassword);
                    return mapper.toDto(savedUser);
                } catch (org.springframework.dao.DataIntegrityViolationException retryException) {
                    log.warn("Violation de contrainte persistante après suppression, chargement de l'utilisateur existant: {}", trimmedEmail);
                    // Continuer pour charger l'utilisateur actif
                }
            }
            
            // Vérifier si un utilisateur actif existe (race condition)
            Query reloadQuery = entityManager.createNativeQuery(
                "SELECT id FROM users WHERE UPPER(email) = UPPER(:email) AND deleted_at IS NULL LIMIT 1"
            );
            reloadQuery.setParameter("email", trimmedEmail);
            
            UUID existingUserId = null;
            try {
                @SuppressWarnings("unchecked")
                List<Object> reloadResults = reloadQuery.getResultList();
                if (!reloadResults.isEmpty() && reloadResults.get(0) != null) {
                    existingUserId = (UUID) reloadResults.get(0);
                }
            } catch (Exception ex) {
                log.error("Erreur lors du rechargement de l'utilisateur avec email {}: {}", trimmedEmail, ex.getMessage());
            }
            
            if (existingUserId == null) {
                log.error("Utilisateur avec email {} existe selon la contrainte mais introuvable en base", trimmedEmail);
                throw new BadRequestException("Un utilisateur avec cet email existe déjà mais n'a pas pu être chargé.");
            }
            
            User existingUser = loadUserByIdWithoutInvalidRoles(existingUserId)
                    .orElseThrow(() -> new BadRequestException("Un utilisateur avec cet email existe déjà."));
            
            log.info("Utilisateur existant {} retourné au lieu de créer un doublon", trimmedEmail);
            return mapper.toDto(existingUser);
        }

        // Envoyer l'email avec le mot de passe temporaire (après le commit de la transaction)
        // S'assurer que l'email est toujours tenté même si une exception se produit après
        sendTemporaryPasswordEmail(trimmedEmail, temporaryPassword);

        log.info("Propriétaire d'entreprise créé avec email: {}", trimmedEmail);
        return mapper.toDto(savedUser);
    }

    /**
     * Crée un associé pour un propriétaire d'entreprise.
     * L'associé aura le rôle BUSINESS_ASSOCIATE et sera lié à la même entreprise que le propriétaire.
     * Vérifie d'abord que le propriétaire a complété les 6 étapes de création de son entreprise.
     */
    @Transactional(noRollbackFor = org.springframework.dao.DataIntegrityViolationException.class)
    public UserDto createAssociateForOwner(UUID ownerId, String associateEmail) {
        entityManager.clear();
        entityManager.flush();
        
        String trimmedEmail = associateEmail.trim();
        log.debug("Tentative de création d'associé pour le propriétaire {} avec email: {}", ownerId, trimmedEmail);
        
        // Charger le propriétaire
        User owner = loadUserByIdWithoutInvalidRoles(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Propriétaire", ownerId));
        
        // Vérifier que c'est bien un BUSINESS_OWNER
        boolean isBusinessOwner = owner.getRoles().stream()
                .anyMatch(role -> role.getType() == RoleType.BUSINESS_OWNER);
        if (!isBusinessOwner) {
            throw new BadRequestException("L'utilisateur spécifié n'est pas un propriétaire d'entreprise.");
        }
        
        // Vérifier que le propriétaire a une entreprise
        if (owner.getBusiness() == null) {
            throw new BadRequestException("Le propriétaire n'a pas d'entreprise associée.");
        }
        
        // Vérifier que l'entreprise a complété les 6 étapes
        if (!businessService.hasCompletedAllSteps(owner.getBusiness().getId())) {
            throw new BadRequestException("Le propriétaire doit compléter les 6 étapes de création de son entreprise avant de pouvoir ajouter un associé.");
        }
        
        // Supprimer définitivement les utilisateurs supprimés avec cet email
        deleteSoftDeletedUserByEmail(trimmedEmail);
        
        // Vérifier si un utilisateur actif existe avec cet email
        UUID foundUserId = null;
        try {
            Query checkUserQuery = entityManager.createNativeQuery(
                "SELECT id FROM users WHERE UPPER(email) = UPPER(:email) AND deleted_at IS NULL LIMIT 1"
            );
            checkUserQuery.setParameter("email", trimmedEmail);
            @SuppressWarnings("unchecked")
            List<Object> results = checkUserQuery.getResultList();
            if (!results.isEmpty() && results.get(0) != null) {
                foundUserId = (UUID) results.get(0);
                log.debug("Utilisateur existant trouvé avec l'email: {} (ID: {})", trimmedEmail, foundUserId);
            }
        } catch (Exception e) {
            log.warn("Erreur lors de la vérification de l'email {}: {}", trimmedEmail, e.getMessage());
        }
        
        // Si un utilisateur existe, le charger et gérer selon son état
        final UUID finalFoundUserId = foundUserId;
        if (finalFoundUserId != null) {
            User existingUser = loadUserByIdWithoutInvalidRoles(finalFoundUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", finalFoundUserId));
            
            // Vérifier si l'utilisateur a déjà le rôle BUSINESS_ASSOCIATE pour cette entreprise
            boolean hasAssociateRole = existingUser.getRoles().stream()
                    .anyMatch(role -> role.getType() == RoleType.BUSINESS_ASSOCIATE);
            boolean isSameBusiness = existingUser.getBusiness() != null && 
                    existingUser.getBusiness().getId().equals(owner.getBusiness().getId());
            
            if (hasAssociateRole && isSameBusiness) {
                log.info("L'utilisateur {} est déjà associé à cette entreprise", trimmedEmail);
                return mapper.toDto(existingUser);
            }
            
            // Vérifier si l'utilisateur est déjà associé à une autre entreprise
            if (hasAssociateRole && existingUser.getBusiness() != null && 
                    !existingUser.getBusiness().getId().equals(owner.getBusiness().getId())) {
                throw new BadRequestException("Cet utilisateur est déjà associé à une autre entreprise. Un associé ne peut être lié qu'à une seule entreprise.");
            }
            
            // Si l'utilisateur existe mais n'a pas le rôle ou n'est pas lié à la bonne entreprise
            Role associateRole = roleRepository.findByType(RoleType.BUSINESS_ASSOCIATE)
                    .orElseThrow(() -> new IllegalStateException("Rôle BUSINESS_ASSOCIATE introuvable."));
            
            String temporaryPassword = generateTemporaryPassword();
            String encodedPassword = passwordEncoder.encode(temporaryPassword);
            
            // Vérifier d'abord si le rôle existe déjà
            Query checkRoleQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = :userId AND role_id = :roleId"
            );
            checkRoleQuery.setParameter("userId", existingUser.getId());
            checkRoleQuery.setParameter("roleId", associateRole.getId());
            Long existingRoleCount = ((Number) checkRoleQuery.getSingleResult()).longValue();
            
            // Ajouter le rôle et mettre à jour l'entreprise et le mot de passe
            if (existingRoleCount == 0) {
                Query insertRoleQuery = entityManager.createNativeQuery(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId)"
                );
                insertRoleQuery.setParameter("userId", existingUser.getId());
                insertRoleQuery.setParameter("roleId", associateRole.getId());
                int rowsInserted = insertRoleQuery.executeUpdate();
                log.info("Rôle BUSINESS_ASSOCIATE ajouté pour l'utilisateur existant {}: {} lignes insérées", 
                        existingUser.getId(), rowsInserted);
            } else {
                log.info("Le rôle BUSINESS_ASSOCIATE existe déjà pour l'utilisateur existant {}", existingUser.getId());
            }
            
            Query updateQuery = entityManager.createNativeQuery(
                "UPDATE users SET business_id = :businessId, password = :password, " +
                "must_change_password = true, enabled = true, updated_at = :updatedAt " +
                "WHERE id = :userId AND deleted_at IS NULL"
            );
            updateQuery.setParameter("businessId", owner.getBusiness().getId());
            updateQuery.setParameter("password", encodedPassword);
            updateQuery.setParameter("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
            updateQuery.setParameter("userId", existingUser.getId());
            int updated = updateQuery.executeUpdate();
            
            if (updated == 0) {
                throw new ResourceNotFoundException("Utilisateur", existingUser.getId());
            }
            
            // Forcer le flush pour s'assurer que les modifications sont persistées
            entityManager.flush();
            
            // Vérifier que le rôle est bien persisté
            Query verifyRoleQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM user_roles ur " +
                "JOIN roles r ON ur.role_id = r.id " +
                "WHERE ur.user_id = :userId AND r.name = 'BUSINESS_ASSOCIATE'"
            );
            verifyRoleQuery.setParameter("userId", existingUser.getId());
            Long verifiedRoleCount = ((Number) verifyRoleQuery.getSingleResult()).longValue();
            log.info("Vérification finale pour utilisateur existant: {} rôles BUSINESS_ASSOCIATE trouvés pour l'utilisateur {}", 
                    verifiedRoleCount, existingUser.getId());
            
            sendTemporaryPasswordEmail(trimmedEmail, temporaryPassword);
            
            entityManager.clear();
            User updatedUser = loadUserByIdWithoutInvalidRoles(existingUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", existingUser.getId()));
            
            log.info("Associé créé/mis à jour pour l'utilisateur: {}", trimmedEmail);
            return mapper.toDto(updatedUser);
        }
        
        // Créer un nouvel utilisateur associé
        Role associateRole = roleRepository.findByType(RoleType.BUSINESS_ASSOCIATE)
                .orElseThrow(() -> new IllegalStateException("Rôle BUSINESS_ASSOCIATE introuvable."));
        
        log.info("Création d'un associé avec email: {} pour l'entreprise: {} avec le rôle: {}", 
                trimmedEmail, owner.getBusiness().getId(), associateRole.getId());
        
        String temporaryPassword = generateTemporaryPassword();
        String encodedPassword = passwordEncoder.encode(temporaryPassword);
        
        User associate = User.builder()
                .email(trimmedEmail)
                .password(encodedPassword)
                .enabled(true)
                .mustChangePassword(true)
                .business(owner.getBusiness())
                .roles(Set.of(associateRole))
                .build();
        
        UUID associateId;
        try {
            // Sauvegarder l'utilisateur d'abord
            User savedAssociate = userRepository.save(associate);
            associateId = savedAssociate.getId();
            log.info("Utilisateur associé sauvegardé avec ID: {}", associateId);
            
            // Forcer le flush pour s'assurer que l'utilisateur est persisté
            entityManager.flush();
            log.info("Flush effectué pour l'utilisateur: {}", associateId);
            
            // IMPORTANT: Insérer TOUJOURS le rôle explicitement avec une requête native
            // pour garantir la persistance même si Hibernate ne le fait pas correctement
            Query insertRoleQuery = entityManager.createNativeQuery(
                "INSERT INTO user_roles (user_id, role_id) " +
                "SELECT :userId, :roleId " +
                "WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = :userId AND role_id = :roleId)"
            );
            insertRoleQuery.setParameter("userId", associateId);
            insertRoleQuery.setParameter("roleId", associateRole.getId());
            int rowsInserted = insertRoleQuery.executeUpdate();
            log.info("Rôle BUSINESS_ASSOCIATE inséré dans user_roles pour l'utilisateur {}: {} lignes insérées", 
                    associateId, rowsInserted);
            
            // Forcer un nouveau flush pour s'assurer que le rôle est bien persisté
            entityManager.flush();
            
            // Vérifier immédiatement que le rôle est bien dans la base
            Query checkRoleQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = :userId AND role_id = :roleId"
            );
            checkRoleQuery.setParameter("userId", associateId);
            checkRoleQuery.setParameter("roleId", associateRole.getId());
            Long existingRoleCount = ((Number) checkRoleQuery.getSingleResult()).longValue();
            log.info("Vérification immédiate: {} rôles trouvés dans user_roles pour l'utilisateur {}", 
                    existingRoleCount, associateId);
            
            if (existingRoleCount == 0) {
                log.error("ERREUR CRITIQUE: Le rôle n'a pas été persisté ! Nouvelle tentative d'insertion...");
                // Réessayer avec une insertion directe sans WHERE NOT EXISTS
                Query retryInsertQuery = entityManager.createNativeQuery(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId)"
                );
                retryInsertQuery.setParameter("userId", associateId);
                retryInsertQuery.setParameter("roleId", associateRole.getId());
                try {
                    int retryRows = retryInsertQuery.executeUpdate();
                    entityManager.flush();
                    log.info("Réessai d'insertion: {} lignes insérées", retryRows);
                } catch (Exception e) {
                    log.error("Erreur lors de la réinsertion du rôle: {}", e.getMessage());
                }
            }
            
            // Vérifier que le rôle est bien persisté
            Query verifyRoleQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM user_roles ur " +
                "JOIN roles r ON ur.role_id = r.id " +
                "WHERE ur.user_id = :userId AND r.name = 'BUSINESS_ASSOCIATE'"
            );
            verifyRoleQuery.setParameter("userId", associateId);
            Long verifiedRoleCount = ((Number) verifyRoleQuery.getSingleResult()).longValue();
            log.info("Vérification finale: {} rôles BUSINESS_ASSOCIATE trouvés pour l'utilisateur {}", 
                    verifiedRoleCount, associateId);
            
            // Vérifier que le business_id est bien stocké
            Query verifyBusinessQuery = entityManager.createNativeQuery(
                "SELECT business_id FROM users WHERE id = :userId AND deleted_at IS NULL"
            );
            verifyBusinessQuery.setParameter("userId", associateId);
            Object businessIdResult = verifyBusinessQuery.getSingleResult();
            log.info("Vérification business_id: business_id = {} pour l'utilisateur {} (attendu: {})", 
                    businessIdResult, associateId, owner.getBusiness().getId());
            
            // Vérification complète: l'associé peut-il être trouvé avec la requête de récupération?
            Query testRetrievalQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM users u " +
                "JOIN user_roles ur ON ur.user_id = u.id " +
                "JOIN roles r ON ur.role_id = r.id " +
                "WHERE r.name = 'BUSINESS_ASSOCIATE' AND u.business_id = :businessId AND u.id = :userId AND u.deleted_at IS NULL"
            );
            testRetrievalQuery.setParameter("businessId", owner.getBusiness().getId());
            testRetrievalQuery.setParameter("userId", associateId);
            Long testRetrievalCount = ((Number) testRetrievalQuery.getSingleResult()).longValue();
            log.info("Test de récupération: {} associés trouvés avec la requête de récupération pour l'utilisateur {}", 
                    testRetrievalCount, associateId);
            
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Violation de contrainte détectée pour {}, chargement de l'utilisateur existant", trimmedEmail);
            entityManager.clear();
            
            Query reloadQuery = entityManager.createNativeQuery(
                "SELECT id FROM users WHERE UPPER(email) = UPPER(:email) AND deleted_at IS NULL LIMIT 1"
            );
            reloadQuery.setParameter("email", trimmedEmail);
            
            UUID existingUserId = null;
            try {
                @SuppressWarnings("unchecked")
                List<Object> reloadResults = reloadQuery.getResultList();
                if (!reloadResults.isEmpty() && reloadResults.get(0) != null) {
                    existingUserId = (UUID) reloadResults.get(0);
                }
            } catch (Exception ex) {
                log.error("Erreur lors du rechargement de l'utilisateur avec email {}: {}", trimmedEmail, ex.getMessage());
            }
            
            if (existingUserId == null) {
                throw new BadRequestException("Un utilisateur avec cet email existe déjà mais n'a pas pu être chargé.");
            }
            
            User existingUser = loadUserByIdWithoutInvalidRoles(existingUserId)
                    .orElseThrow(() -> new BadRequestException("Un utilisateur avec cet email existe déjà."));
            
            return mapper.toDto(existingUser);
        }
        
        sendTemporaryPasswordEmail(trimmedEmail, temporaryPassword);
        
        // Recharger l'associé depuis la base de données pour s'assurer que toutes les relations sont chargées
        entityManager.clear();
        User reloadedAssociate = loadUserByIdWithoutInvalidRoles(associateId)
                .orElseThrow(() -> new ResourceNotFoundException("Associé", associateId));
        
        log.info("Associé créé avec email: {} pour le propriétaire {}", trimmedEmail, ownerId);
        return mapper.toDto(reloadedAssociate);
    }

    /**
     * Change le mot de passe temporaire d'un utilisateur.
     */
    @Transactional
    public UserDto changeTemporaryPassword(UUID userId, String currentPassword, String newPassword) {
        // Vider le cache pour s'assurer de charger les données fraîches depuis la base de données
        entityManager.clear();
        
        User user = loadUserByIdWithoutInvalidRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));

        if (!user.isEnabled()) {
            throw new BadRequestException("Le compte n'est pas activé.");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Mot de passe actuel incorrect.");
        }

        if (!user.isMustChangePassword()) {
            throw new BadRequestException("Ce compte n'a pas besoin de changer son mot de passe.");
        }

        // Sauvegarder les informations importantes avant la mise à jour
        boolean hadAssociateRole = user.getRoles().stream()
                .anyMatch(role -> role.getType() == RoleType.BUSINESS_ASSOCIATE);
        UUID businessId = user.getBusiness() != null ? user.getBusiness().getId() : null;

        String encodedNewPassword = passwordEncoder.encode(newPassword);
        
        Query updateQuery = entityManager.createNativeQuery(
            "UPDATE users SET password = :password, must_change_password = false, updated_at = :updatedAt " +
            "WHERE id = :userId AND deleted_at IS NULL"
        );
        updateQuery.setParameter("password", encodedNewPassword);
        updateQuery.setParameter("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        updateQuery.setParameter("userId", userId);
        int updated = updateQuery.executeUpdate();

        if (updated == 0) {
            throw new ResourceNotFoundException("Utilisateur", userId);
        }

        // S'assurer que le rôle BUSINESS_ASSOCIATE est toujours présent si l'utilisateur l'avait avant
        // et qu'il a toujours un business_id associé
        if (hadAssociateRole && businessId != null) {
            Role associateRole = roleRepository.findByType(RoleType.BUSINESS_ASSOCIATE)
                    .orElseThrow(() -> new IllegalStateException("Rôle BUSINESS_ASSOCIATE introuvable."));
            
            // Vérifier et restaurer le rôle BUSINESS_ASSOCIATE si nécessaire
            Query checkRoleQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = :userId AND role_id = :roleId"
            );
            checkRoleQuery.setParameter("userId", userId);
            checkRoleQuery.setParameter("roleId", associateRole.getId());
            Long roleCount = ((Number) checkRoleQuery.getSingleResult()).longValue();
            
            if (roleCount == 0) {
                // Le rôle a été perdu, le restaurer
                Query insertRoleQuery = entityManager.createNativeQuery(
                    "INSERT INTO user_roles (user_id, role_id) VALUES (:userId, :roleId)"
                );
                insertRoleQuery.setParameter("userId", userId);
                insertRoleQuery.setParameter("roleId", associateRole.getId());
                insertRoleQuery.executeUpdate();
                entityManager.flush();
                log.warn("Rôle BUSINESS_ASSOCIATE restauré pour l'utilisateur {} après changement de mot de passe", user.getEmail());
            }
            
            // S'assurer que le business_id est toujours présent
            Query checkBusinessQuery = entityManager.createNativeQuery(
                "SELECT business_id FROM users WHERE id = :userId AND deleted_at IS NULL"
            );
            checkBusinessQuery.setParameter("userId", userId);
            Object businessIdResult = checkBusinessQuery.getSingleResult();
            
            if (businessIdResult == null || !businessIdResult.equals(businessId)) {
                // Le business_id a été perdu, le restaurer
                Query updateBusinessQuery = entityManager.createNativeQuery(
                    "UPDATE users SET business_id = :businessId, updated_at = :updatedAt " +
                    "WHERE id = :userId AND deleted_at IS NULL"
                );
                updateBusinessQuery.setParameter("businessId", businessId);
                updateBusinessQuery.setParameter("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
                updateBusinessQuery.setParameter("userId", userId);
                updateBusinessQuery.executeUpdate();
                entityManager.flush();
                log.warn("business_id restauré pour l'utilisateur {} après changement de mot de passe", user.getEmail());
            }
        }

        // Recharger l'utilisateur pour s'assurer que toutes les données sont à jour (rôles, business, etc.)
        entityManager.clear();
        User updatedUser = loadUserByIdWithoutInvalidRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));
        
        log.info("Mot de passe temporaire changé pour l'utilisateur: {}", updatedUser.getEmail());
        return mapper.toDto(updatedUser);
    }

    /**
     * Met à jour un utilisateur (businessId, firstname, lastname).
     * Utilise une requête native pour éviter les conflits d'optimistic locking.
     */
    @Transactional
    public UserDto update(UUID userId, UUID businessId, String firstname, String lastname) {
        if (businessId != null) {
            businessRepository.findById(businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Entreprise", businessId));
        }
        if (!loadUserByIdWithoutInvalidRoles(userId).isPresent()) {
            throw new ResourceNotFoundException("Utilisateur", userId);
        }

        StringBuilder sql = new StringBuilder("UPDATE users SET updated_at = :updatedAt, version = version + 1");
        if (businessId != null || (firstname != null || lastname != null)) {
            if (businessId != null) sql.append(", business_id = :businessId");
            if (firstname != null) sql.append(", firstname = :firstname");
            if (lastname != null) sql.append(", lastname = :lastname");
        }
        sql.append(" WHERE id = :userId AND deleted_at IS NULL");

        Query updateQuery = entityManager.createNativeQuery(sql.toString());
        updateQuery.setParameter("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        updateQuery.setParameter("userId", userId);
        if (businessId != null) updateQuery.setParameter("businessId", businessId);
        if (firstname != null) updateQuery.setParameter("firstname", firstname.trim());
        if (lastname != null) updateQuery.setParameter("lastname", lastname.trim());
        int updated = updateQuery.executeUpdate();

        if (updated == 0) {
            throw new ResourceNotFoundException("Utilisateur", userId);
        }

        return loadUserByIdWithoutInvalidRoles(userId)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));
    }

    /**
     * Met à jour l'entreprise d'un utilisateur (rétrocompatibilité).
     */
    public UserDto updateBusiness(UUID userId, UUID businessId) {
        return update(userId, businessId, null, null);
    }

    /**
     * Désactive le compte d'un utilisateur (soft disable).
     */
    @Transactional
    public UserDto disableAccount(UUID userId) {
        return updateAccountStatus(userId, false);
    }

    /**
     * Active le compte d'un utilisateur.
     */
    @Transactional
    public UserDto enableAccount(UUID userId) {
        return updateAccountStatus(userId, true);
    }

    /**
     * Met à jour le statut d'activation d'un compte utilisateur.
     * Méthode centralisée pour éviter la duplication de code.
     */
    private UserDto updateAccountStatus(UUID userId, boolean enabled) {
        entityManager.clear();
        User user = loadUserByIdWithoutInvalidRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));
        
        Query updateQuery = entityManager.createNativeQuery(
            "UPDATE users SET enabled = :enabled, updated_at = :updatedAt " +
            "WHERE id = :userId AND deleted_at IS NULL"
        );
        updateQuery.setParameter("enabled", enabled);
        updateQuery.setParameter("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        updateQuery.setParameter("userId", userId);
        int updated = updateQuery.executeUpdate();
        
        if (updated == 0) {
            throw new ResourceNotFoundException("Utilisateur", userId);
        }
        
        user.setEnabled(enabled);
        log.info("Compte {} pour l'utilisateur: {}", enabled ? "activé" : "désactivé", user.getEmail());
        return mapper.toDto(user);
    }

    /**
     * Retire le rôle BUSINESS_ASSOCIATE d'un utilisateur (le retire en tant qu'associé).
     * Ne supprime pas le compte utilisateur, seulement le rôle d'associé.
     */
    @Transactional
    public UserDto removeAssociateRole(UUID associateId) {
        entityManager.clear();
        
        User associate = loadUserByIdWithoutInvalidRoles(associateId)
                .orElseThrow(() -> new ResourceNotFoundException("Associé", associateId));
        
        // Vérifier que l'utilisateur a bien le rôle BUSINESS_ASSOCIATE
        boolean hasAssociateRole = associate.getRoles().stream()
                .anyMatch(role -> role.getType() == RoleType.BUSINESS_ASSOCIATE);
        
        if (!hasAssociateRole) {
            throw new BadRequestException("Cet utilisateur n'est pas un associé.");
        }
        
        Role associateRole = roleRepository.findByType(RoleType.BUSINESS_ASSOCIATE)
                .orElseThrow(() -> new IllegalStateException("Rôle BUSINESS_ASSOCIATE introuvable."));
        
        // Retirer le rôle BUSINESS_ASSOCIATE
        Query deleteRoleQuery = entityManager.createNativeQuery(
            "DELETE FROM user_roles WHERE user_id = :userId AND role_id = :roleId"
        );
        deleteRoleQuery.setParameter("userId", associateId);
        deleteRoleQuery.setParameter("roleId", associateRole.getId());
        int deleted = deleteRoleQuery.executeUpdate();
        
        if (deleted == 0) {
            throw new BadRequestException("Impossible de retirer le rôle d'associé.");
        }
        
        // Mettre à jour le business_id à null car l'utilisateur n'est plus associé à une entreprise
        Query updateBusinessQuery = entityManager.createNativeQuery(
            "UPDATE users SET business_id = NULL, updated_at = :updatedAt " +
            "WHERE id = :userId AND deleted_at IS NULL"
        );
        updateBusinessQuery.setParameter("updatedAt", Timestamp.valueOf(LocalDateTime.now()));
        updateBusinessQuery.setParameter("userId", associateId);
        updateBusinessQuery.executeUpdate();
        
        // Recharger l'utilisateur pour retourner les données à jour
        entityManager.clear();
        User updatedUser = loadUserByIdWithoutInvalidRoles(associateId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", associateId));
        
        log.info("Rôle d'associé retiré pour l'utilisateur: {}", updatedUser.getEmail());
        return mapper.toDto(updatedUser);
    }

    /**
     * Supprime définitivement le compte d'un utilisateur de la base de données (hard delete).
     * Fonctionne même si l'utilisateur est déjà soft deleted.
     */
    @Transactional
    public void deleteAccount(UUID userId) {
        entityManager.clear();
        
        // Vérifier si l'utilisateur existe (actif ou supprimé)
        Query checkUserQuery = entityManager.createNativeQuery(
            "SELECT email FROM users WHERE id = :userId LIMIT 1"
        );
        checkUserQuery.setParameter("userId", userId);
        
        String userEmail = null;
        try {
            Object result = checkUserQuery.getSingleResult();
            if (result != null) {
                userEmail = (String) result;
            }
        } catch (jakarta.persistence.NoResultException e) {
            throw new ResourceNotFoundException("Utilisateur", userId);
        }
        
        if (userEmail == null) {
            throw new ResourceNotFoundException("Utilisateur", userId);
        }
        
        // Supprimer d'abord les relations dans user_roles (même si l'utilisateur est soft deleted)
        Query deleteRolesQuery = entityManager.createNativeQuery(
            "DELETE FROM user_roles WHERE user_id = :userId"
        );
        deleteRolesQuery.setParameter("userId", userId);
        deleteRolesQuery.executeUpdate();
        
        // Supprimer définitivement l'utilisateur de la base de données (même s'il est soft deleted)
        Query deleteUserQuery = entityManager.createNativeQuery(
            "DELETE FROM users WHERE id = :userId"
        );
        deleteUserQuery.setParameter("userId", userId);
        int deleted = deleteUserQuery.executeUpdate();
        
        if (deleted == 0) {
            throw new ResourceNotFoundException("Utilisateur", userId);
        }
        
        log.info("Compte définitivement supprimé de la base de données pour l'utilisateur: {}", userEmail);
    }

    /**
     * Supprime définitivement un utilisateur supprimé (soft delete) par son email.
     * Méthode utilitaire pour éviter la duplication de code.
     * Retourne true si un utilisateur supprimé a été trouvé et supprimé, false sinon.
     */
    private boolean deleteSoftDeletedUserByEmail(String email) {
        Query checkDeletedQuery = entityManager.createNativeQuery(
            "SELECT id FROM users WHERE UPPER(email) = UPPER(:email) AND deleted_at IS NOT NULL LIMIT 1"
        );
        checkDeletedQuery.setParameter("email", email);
        
        try {
            @SuppressWarnings("unchecked")
            List<Object> deletedResults = checkDeletedQuery.getResultList();
            if (!deletedResults.isEmpty() && deletedResults.get(0) != null) {
                UUID deletedUserId = (UUID) deletedResults.get(0);
                log.info("Utilisateur supprimé trouvé, suppression définitive pour permettre la réutilisation de l'email: {}", email);
                
                // Supprimer définitivement les relations dans user_roles
                Query deleteRolesQuery = entityManager.createNativeQuery(
                    "DELETE FROM user_roles WHERE user_id = :userId"
                );
                deleteRolesQuery.setParameter("userId", deletedUserId);
                deleteRolesQuery.executeUpdate();
                
                // Supprimer définitivement l'utilisateur
                Query deleteUserQuery = entityManager.createNativeQuery(
                    "DELETE FROM users WHERE id = :userId"
                );
                deleteUserQuery.setParameter("userId", deletedUserId);
                deleteUserQuery.executeUpdate();
                
                entityManager.flush();
                return true;
            }
        } catch (Exception e) {
            log.debug("Aucun utilisateur supprimé trouvé pour: {}", email);
        }
        return false;
    }

    /**
     * Confirme le code de connexion et retourne l'utilisateur activé.
     * Vérifie toujours les données fraîches depuis la base de données pour permettre
     * plusieurs tentatives avec le même code tant qu'il n'a pas expiré.
     */
    @Transactional
    public User confirmLogin(UUID userId, String code) {
        // Vider le cache de l'EntityManager pour s'assurer de charger les données fraîches
        entityManager.clear();
        
        User user = loadUserByIdWithoutInvalidRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));
        
        if (!user.isEnabled()) {
            throw new BadRequestException("Le compte n'est pas activé.");
        }
        
        if (user.getVerificationCode() == null || user.getCodeExpiration() == null) {
            throw new BadRequestException("Aucun code de vérification en attente.");
        }
        
        // Vérifier l'expiration AVANT de vérifier le code pour un message d'erreur plus clair
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(user.getCodeExpiration())) {
            throw new BadRequestException("Code expiré.");
        }
        
        // Vérifier le code après avoir vérifié l'expiration
        if (!code.equals(user.getVerificationCode())) {
            throw new BadRequestException("Code invalide.");
        }
        
        // Mettre à jour via requête native pour éviter le chargement EAGER des rôles invalides
        // Invalider le code après utilisation réussie
        Query updateQuery = entityManager.createNativeQuery(
            "UPDATE users SET verification_code = NULL, code_expiration = NULL, " +
            "resend_attempts = 0, updated_at = :updatedAt " +
            "WHERE id = :userId AND deleted_at IS NULL"
        );
        updateQuery.setParameter("updatedAt", Timestamp.valueOf(now));
        updateQuery.setParameter("userId", userId);
        int updated = updateQuery.executeUpdate();
        
        if (updated == 0) {
            throw new ResourceNotFoundException("Utilisateur", userId);
        }
        
        // Mettre à jour l'objet user pour le retour
        user.setVerificationCode(null);
        user.setCodeExpiration(null);
        user.setResendAttempts(0);
        
        return user;
    }

    private String generateVerificationCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    /**
     * Génère un mot de passe temporaire alphanumérique de 10 caractères.
     */
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    /**
     * Envoie un email avec le mot de passe temporaire à l'utilisateur.
     * Méthode centralisée pour éviter la duplication de code.
     * Cette méthode ne propage pas les exceptions pour ne pas faire échouer la création de l'utilisateur.
     */
    private void sendTemporaryPasswordEmail(String email, String temporaryPassword) {
        String loginUrl = frontendUrl + "/auth/login";
        try {
            mailService.sendTemporaryPassword(email, temporaryPassword, loginUrl);
            log.info("Email avec mot de passe temporaire envoyé avec succès à: {}", email);
        } catch (Exception e) {
            log.error("ÉCHEC de l'envoi de l'email avec le mot de passe temporaire à {}: {}", email, e.getMessage(), e);
            // Ne pas faire échouer l'opération si l'email échoue, mais logger l'erreur avec tous les détails
            // L'utilisateur est créé mais n'a pas reçu l'email - l'administrateur devra le contacter manuellement
        }
    }

    @Transactional
    public void setVerificationCode(UUID userId, String code, LocalDateTime expiration) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setVerificationCode(code);
            u.setCodeExpiration(expiration);
            userRepository.save(u);
        });
    }

    @Transactional
    public UserDto confirmCodeAndEnable(UUID userId, String code) {
        User u = userRepository.findById(userId).orElse(null);
        if (u == null || !code.equals(u.getVerificationCode()) || u.getCodeExpiration() == null || LocalDateTime.now().isAfter(u.getCodeExpiration()))
            return null;
        u.setVerificationCode(null);
        u.setCodeExpiration(null);
        u.setEnabled(true);
        return mapper.toDto(userRepository.save(u));
    }

    /**
     * Vérifie si un email existe dans la base de données (utilisateur actif ou par défaut).
     */
    @Transactional(readOnly = true)
    public EmailCheckResponse checkEmailExists(String email) {
        User user = loadUserWithoutInvalidRolesReadOnly(email.trim());
        if (user != null) {
            return new EmailCheckResponse(true, user.getId(), user.getEmail());
        }
        return new EmailCheckResponse(false, null, email.trim());
    }

    /**
     * Authentifie un utilisateur avec son mot de passe et envoie un code OTP par email.
     * Génère un nouveau code qui remplace l'ancien code s'il existe.
     * Si l'utilisateur doit changer son mot de passe temporaire, une exception est levée.
     */
    @Transactional
    public UserDto authenticateWithPassword(UUID userId, String password) {
        // Vider le cache pour s'assurer de charger les données fraîches
        entityManager.clear();
        
        User user = loadUserByIdWithoutInvalidRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));
        
        if (!user.isEnabled()) {
            throw new BadRequestException("Le compte n'est pas activé.");
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Mot de passe incorrect.");
        }
        
        // Si l'utilisateur doit changer son mot de passe temporaire, il ne peut pas utiliser le flux OTP
        if (user.isMustChangePassword()) {
            throw new BadRequestException("Vous devez changer votre mot de passe temporaire avant de continuer.");
        }
        
        // Générer un nouveau code OTP qui remplace l'ancien
        String code = generateVerificationCode();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = now.plusMinutes(CODE_VALIDITY_MINUTES);
        
        // Mettre à jour via requête native pour éviter le chargement EAGER des rôles invalides
        // L'ancien code est automatiquement remplacé par le nouveau
        Query updateQuery = entityManager.createNativeQuery(
            "UPDATE users SET verification_code = :code, code_expiration = :expiration, " +
            "last_code_sent_at = :lastSentAt, resend_attempts = 0, updated_at = :updatedAt " +
            "WHERE id = :userId AND deleted_at IS NULL"
        );
        updateQuery.setParameter("code", code);
        updateQuery.setParameter("expiration", Timestamp.valueOf(expiration));
        updateQuery.setParameter("lastSentAt", Timestamp.valueOf(now));
        updateQuery.setParameter("updatedAt", Timestamp.valueOf(now));
        updateQuery.setParameter("userId", userId);
        int updated = updateQuery.executeUpdate();
        
        if (updated == 0) {
            throw new ResourceNotFoundException("Utilisateur", userId);
        }
        
        // Mettre à jour l'objet user pour le retour
        user.setVerificationCode(code);
        user.setCodeExpiration(expiration);
        user.setLastCodeSentAt(now);
        user.setResendAttempts(0);
        
        // Envoyer l'email de manière asynchrone pour ne pas bloquer la réponse
        try {
            mailService.sendVerificationCode(user.getEmail(), code, CODE_VALIDITY_MINUTES);
        } catch (Exception e) {
            log.warn("Erreur lors de l'envoi de l'email de vérification à {}: {}", user.getEmail(), e.getMessage());
            // Ne pas faire échouer la requête si l'email échoue
        }
        
        return mapper.toDto(user);
    }

    /**
     * Authentifie directement un utilisateur avec son email et mot de passe.
     * Si l'utilisateur doit changer son mot de passe temporaire, retourne l'utilisateur avec mustChangePassword=true.
     */
    @Transactional
    public User authenticateDirectlyWithPassword(String email, String password) {
        entityManager.clear();
        
        User user = loadUserWithoutInvalidRoles(email.trim());
        if (user == null) {
            throw new ResourceNotFoundException("Aucun compte avec cet email.");
        }
        
        if (!user.isEnabled()) {
            throw new BadRequestException("Le compte n'est pas activé.");
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("Mot de passe incorrect.");
        }
        
        return user;
    }

    /**
     * Génère un code à 6 chiffres, le sauvegarde sur l'utilisateur et envoie l'email via Brevo (ou log).
     * Vérifie les limitations : 30 secondes entre chaque envoi, maximum 3 tentatives.
     * L'ancien code est automatiquement désactivé et remplacé par le nouveau code.
     */
    @Transactional
    public UserDto requestLoginCode(String email) {
        // Vider le cache pour s'assurer de charger les données fraîches
        entityManager.clear();
        
        User u = loadUserWithoutInvalidRoles(email.trim());
        if (u == null) {
            throw new ResourceNotFoundException("Aucun compte avec cet email.");
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // Vérifier le délai de 30 secondes
        if (u.getLastCodeSentAt() != null) {
            long secondsSinceLastSent = java.time.Duration.between(u.getLastCodeSentAt(), now).getSeconds();
            if (secondsSinceLastSent < RESEND_COOLDOWN_SECONDS) {
                long remainingSeconds = RESEND_COOLDOWN_SECONDS - secondsSinceLastSent;
                throw new BadRequestException(String.format("Veuillez attendre %d seconde(s) avant de renvoyer le code.", remainingSeconds));
            }
        }
        
        // Vérifier le nombre maximum de tentatives
        if (u.getResendAttempts() != null && u.getResendAttempts() >= MAX_RESEND_ATTEMPTS) {
            throw new BadRequestException("Nombre maximum de tentatives de renvoi atteint. Veuillez réessayer plus tard.");
        }
        
        // Générer un nouveau code qui remplace l'ancien
        String code = generateVerificationCode();
        LocalDateTime exp = now.plusMinutes(CODE_VALIDITY_MINUTES);
        int newResendAttempts = (u.getResendAttempts() == null ? 0 : u.getResendAttempts()) + 1;
        
        // Mettre à jour via requête native pour éviter le chargement EAGER des rôles invalides
        // L'ancien code est automatiquement remplacé par le nouveau dans la base de données
        Query updateQuery = entityManager.createNativeQuery(
            "UPDATE users SET verification_code = :code, code_expiration = :expiration, " +
            "last_code_sent_at = :lastSentAt, resend_attempts = :resendAttempts, updated_at = :updatedAt " +
            "WHERE id = :userId AND deleted_at IS NULL"
        );
        updateQuery.setParameter("code", code);
        updateQuery.setParameter("expiration", Timestamp.valueOf(exp));
        updateQuery.setParameter("lastSentAt", Timestamp.valueOf(now));
        updateQuery.setParameter("resendAttempts", newResendAttempts);
        updateQuery.setParameter("updatedAt", Timestamp.valueOf(now));
        updateQuery.setParameter("userId", u.getId());
        int updated = updateQuery.executeUpdate();
        
        if (updated == 0) {
            throw new ResourceNotFoundException("Aucun compte avec cet email.");
        }
        
        // Mettre à jour l'objet user pour le retour
        u.setVerificationCode(code);
        u.setCodeExpiration(exp);
        u.setLastCodeSentAt(now);
        u.setResendAttempts(newResendAttempts);
        
        // Envoyer l'email de manière asynchrone pour ne pas bloquer la réponse
        try {
            mailService.sendVerificationCode(u.getEmail(), code, CODE_VALIDITY_MINUTES);
        } catch (Exception e) {
            log.warn("Erreur lors de l'envoi de l'email de vérification à {}: {}", u.getEmail(), e.getMessage());
            // Ne pas faire échouer la requête si l'email échoue
        }
        
        return mapper.toDto(u);
    }

    /**
     * Corrige les rôles invalides (ADMIN) en les supprimant de la table user_roles.
     * S'exécute dans une transaction séparée pour éviter les conflits avec les transactions readOnly.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void fixInvalidRoles(String email) {
        try {
            Query query = entityManager.createNativeQuery(
                "DELETE FROM user_roles ur " +
                "USING users u, roles r " +
                "WHERE ur.user_id = u.id " +
                "AND ur.role_id = r.id " +
                "AND UPPER(u.email) = UPPER(:email) " +
                "AND r.name NOT IN ('SUPER_ADMIN', 'BUSINESS_OWNER', 'BUSINESS_ASSOCIATE')"
            );
            query.setParameter("email", email);
            int deleted = query.executeUpdate();
            if (deleted > 0) {
                log.debug("Rôles invalides supprimés pour l'utilisateur: {}", email);
            }
            entityManager.flush();
            entityManager.clear();
        } catch (Exception e) {
            log.warn("Erreur lors de la correction des rôles invalides pour {}: {}", email, e.getMessage());
        }
    }

    /**
     * Charge un utilisateur par email sans charger les rôles invalides pour éviter les erreurs d'enum.
     * Version pour les méthodes readOnly (ne corrige pas les rôles, seulement les charge).
     */
    private User loadUserWithoutInvalidRolesReadOnly(String email) {
        try {
            Query nativeQuery = entityManager.createNativeQuery(
                "SELECT id, firstname, lastname, email, password, enabled, business_id, " +
                "verification_code, code_expiration, must_change_password, created_at, updated_at, version, deleted_at " +
                "FROM users WHERE UPPER(email) = UPPER(:email) AND deleted_at IS NULL LIMIT 1"
            );
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
                    .mustChangePassword(result[9] != null && ((Boolean) result[9]))
                    .build();
            
            // Charger seulement les rôles valides (sans corriger les invalides)
            Set<Role> validRoles = loadValidRoles(user.getId());
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
     * Charge un utilisateur par email sans charger les rôles invalides pour éviter les erreurs d'enum.
     * Version pour les méthodes qui modifient l'utilisateur (corrige aussi les rôles invalides).
     */
    private User loadUserWithoutInvalidRoles(String email) {
        fixInvalidRoles(email);
        
        try {
            Query nativeQuery = entityManager.createNativeQuery(
                "SELECT id, firstname, lastname, email, password, enabled, business_id, " +
                "verification_code, code_expiration, last_code_sent_at, resend_attempts, " +
                "must_change_password, created_at, updated_at, version, deleted_at " +
                "FROM users WHERE UPPER(email) = UPPER(:email) AND deleted_at IS NULL LIMIT 1"
            );
            nativeQuery.setParameter("email", email);
            Object[] result = (Object[]) nativeQuery.getSingleResult();
            if (result == null) {
                return null;
            }
            
            UUID businessId = result[6] != null ? (UUID) result[6] : null;
            Business business = businessId != null ? businessRepository.findById(businessId).orElse(null) : null;
            
            User user = User.builder()
                    .id((UUID) result[0])
                    .firstname((String) result[1])
                    .lastname((String) result[2])
                    .email((String) result[3])
                    .password((String) result[4])
                    .enabled((Boolean) result[5])
                    .business(business)
                    .verificationCode((String) result[7])
                    .codeExpiration(convertToLocalDateTime(result[8]))
                    .lastCodeSentAt(convertToLocalDateTime(result[9]))
                    .resendAttempts(result[10] != null ? ((Number) result[10]).intValue() : 0)
                    .mustChangePassword(result[11] != null && ((Boolean) result[11]))
                    .build();
            
            // Charger seulement les rôles valides
            Set<Role> validRoles = loadValidRoles(user.getId());
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
     * Charge un utilisateur par ID sans charger les rôles invalides pour éviter les erreurs d'enum.
     */
    private java.util.Optional<User> loadUserByIdWithoutInvalidRoles(UUID userId) {
        try {
            Query nativeQuery = entityManager.createNativeQuery(
                "SELECT id, firstname, lastname, email, password, enabled, business_id, " +
                "verification_code, code_expiration, last_code_sent_at, resend_attempts, " +
                "must_change_password, created_at, updated_at, version, deleted_at " +
                "FROM users WHERE id = :userId AND deleted_at IS NULL LIMIT 1"
            );
            nativeQuery.setParameter("userId", userId);
            Object[] result = (Object[]) nativeQuery.getSingleResult();
            if (result == null) {
                return java.util.Optional.empty();
            }
            
            UUID businessId = result[6] != null ? (UUID) result[6] : null;
            Business business = null;
            if (businessId != null) {
                business = businessRepository.findById(businessId).orElse(null);
            }
            
            User user = User.builder()
                    .id((UUID) result[0])
                    .firstname((String) result[1])
                    .lastname((String) result[2])
                    .email((String) result[3])
                    .password((String) result[4])
                    .enabled((Boolean) result[5])
                    .business(business)
                    .verificationCode((String) result[7])
                    .codeExpiration(convertToLocalDateTime(result[8]))
                    .lastCodeSentAt(convertToLocalDateTime(result[9]))
                    .resendAttempts(result[10] != null ? ((Number) result[10]).intValue() : 0)
                    .mustChangePassword(result[11] != null && ((Boolean) result[11]))
                    .build();
            
            // Charger seulement les rôles valides
            Set<Role> validRoles = loadValidRoles(user.getId());
            user.setRoles(validRoles);
            
            return java.util.Optional.of(user);
        } catch (jakarta.persistence.NoResultException e) {
            return java.util.Optional.empty();
        } catch (Exception e) {
            log.warn("Erreur lors du chargement de l'utilisateur {}: {}", userId, e.getMessage());
            return java.util.Optional.empty();
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

    /**
     * Restaure les rôles BUSINESS_ASSOCIATE manquants pour les utilisateurs d'une entreprise.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreMissingAssociateRoles(UUID businessId, UUID associateRoleId) {
        try {
            // Trouver les utilisateurs qui ont le business_id mais pas le rôle BUSINESS_ASSOCIATE et ne sont pas des propriétaires
            Query findMissingQuery = entityManager.createNativeQuery(
                "SELECT u.id, u.email " +
                "FROM users u " +
                "WHERE u.business_id = :businessId " +
                "AND u.deleted_at IS NULL " +
                "AND u.id NOT IN (" +
                "  SELECT ur.user_id FROM user_roles ur " +
                "  JOIN roles r ON ur.role_id = r.id " +
                "  WHERE r.name IN ('BUSINESS_OWNER', 'BUSINESS_ASSOCIATE')" +
                ")"
            );
            findMissingQuery.setParameter("businessId", businessId);
            @SuppressWarnings("unchecked")
            List<Object[]> missingUsers = findMissingQuery.getResultList();
            
            if (!missingUsers.isEmpty()) {
                log.warn("Trouvé {} utilisateurs avec business_id {} mais sans rôle BUSINESS_ASSOCIATE. Restauration...", 
                        missingUsers.size(), businessId);
                
                for (Object[] userRow : missingUsers) {
                    UUID userId = (UUID) userRow[0];
                    String userEmail = (String) userRow[1];
                    
                    Query insertQuery = entityManager.createNativeQuery(
                        "INSERT INTO user_roles (user_id, role_id) " +
                        "SELECT :userId, :roleId " +
                        "WHERE NOT EXISTS (SELECT 1 FROM user_roles WHERE user_id = :userId AND role_id = :roleId)"
                    );
                    insertQuery.setParameter("userId", userId);
                    insertQuery.setParameter("roleId", associateRoleId);
                    int rowsInserted = insertQuery.executeUpdate();
                    entityManager.flush();
                    log.info("Rôle BUSINESS_ASSOCIATE restauré pour l'utilisateur {} ({}): {} lignes insérées", 
                            userEmail, userId, rowsInserted);
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la restauration des rôles BUSINESS_ASSOCIATE pour l'entreprise {}: {}", 
                    businessId, e.getMessage());
        }
    }

    /**
     * Charge uniquement les rôles valides pour un utilisateur.
     */
    private Set<Role> loadValidRoles(UUID userId) {
        try {
            Query rolesQuery = entityManager.createNativeQuery(
                "SELECT r.id, r.name FROM roles r " +
                "JOIN user_roles ur ON ur.role_id = r.id " +
                "WHERE ur.user_id = :userId AND r.name IN ('SUPER_ADMIN', 'BUSINESS_OWNER', 'BUSINESS_ASSOCIATE')"
            );
            rolesQuery.setParameter("userId", userId);
            @SuppressWarnings("unchecked")
            java.util.List<Object[]> roleResults = rolesQuery.getResultList();
            
            return roleResults.stream()
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
        } catch (Exception e) {
            log.warn("Erreur lors du chargement des rôles pour l'utilisateur {}: {}", userId, e.getMessage());
            return Set.of();
        }
    }
}
