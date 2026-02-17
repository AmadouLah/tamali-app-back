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

    @Transactional
    public UserDto create(String firstname, String lastname, String email, String password, UUID businessId, Set<RoleType> roleTypes) {
        if (userRepository.existsByEmail(email)) return null;
        Business business = businessId != null ? businessRepository.findById(businessId).orElse(null) : null;
        Set<Role> roles = roleTypes != null && !roleTypes.isEmpty()
                ? roleTypes.stream()
                .flatMap(rt -> roleRepository.findByType(rt).stream())
                .collect(java.util.stream.Collectors.toSet())
                : Set.of();
        User u = User.builder()
                .firstname(firstname)
                .lastname(lastname)
                .email(email)
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
     */
    @Transactional
    public UserDto createBusinessOwnerWithEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Un compte existe déjà avec cet email.");
        }

        Role businessOwnerRole = roleRepository.findByType(RoleType.BUSINESS_OWNER)
                .orElseThrow(() -> new IllegalStateException("Rôle BUSINESS_OWNER introuvable."));

        String temporaryPassword = generateTemporaryPassword();
        String encodedPassword = passwordEncoder.encode(temporaryPassword);

        User user = User.builder()
                .email(email.trim())
                .password(encodedPassword)
                .enabled(true)
                .mustChangePassword(true)
                .business(null)
                .roles(Set.of(businessOwnerRole))
                .build();

        user = userRepository.save(user);

        String loginUrl = frontendUrl + "/auth/login";
        try {
            mailService.sendTemporaryPassword(email, temporaryPassword, loginUrl);
        } catch (Exception e) {
            log.warn("Erreur lors de l'envoi de l'email avec le mot de passe temporaire à {}: {}", email, e.getMessage());
            // Ne pas faire échouer la création si l'email échoue
        }

        log.info("Propriétaire d'entreprise créé avec email: {}", email);
        return mapper.toDto(user);
    }

    /**
     * Change le mot de passe temporaire d'un utilisateur.
     */
    @Transactional
    public UserDto changeTemporaryPassword(UUID userId, String currentPassword, String newPassword) {
        User user = loadUserByIdWithoutInvalidRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Mot de passe actuel incorrect.");
        }

        if (!user.isMustChangePassword()) {
            throw new BadRequestException("Ce compte n'a pas besoin de changer son mot de passe.");
        }

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

        user.setPassword(encodedNewPassword);
        user.setMustChangePassword(false);

        log.info("Mot de passe temporaire changé pour l'utilisateur: {}", user.getEmail());
        return mapper.toDto(user);
    }

    /**
     * Met à jour l'entreprise d'un utilisateur.
     */
    @Transactional
    public UserDto updateBusiness(UUID userId, UUID businessId) {
        User user = loadUserByIdWithoutInvalidRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));
        
        if (businessId != null) {
            Business business = businessRepository.findById(businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Entreprise", businessId));
            user.setBusiness(business);
        } else {
            user.setBusiness(null);
        }
        
        return mapper.toDto(userRepository.save(user));
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
                "AND r.name NOT IN ('SUPER_ADMIN', 'BUSINESS_OWNER')"
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
                "verification_code, code_expiration, created_at, updated_at, version, deleted_at " +
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
                "created_at, updated_at, version, deleted_at " +
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
                    .lastCodeSentAt(convertToLocalDateTime(result[9]))
                    .resendAttempts(result[10] != null ? ((Number) result[10]).intValue() : 0)
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
                "created_at, updated_at, version, deleted_at " +
                "FROM users WHERE id = :userId AND deleted_at IS NULL LIMIT 1"
            );
            nativeQuery.setParameter("userId", userId);
            Object[] result = (Object[]) nativeQuery.getSingleResult();
            if (result == null) {
                return java.util.Optional.empty();
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
                    .lastCodeSentAt(convertToLocalDateTime(result[9]))
                    .resendAttempts(result[10] != null ? ((Number) result[10]).intValue() : 0)
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
     * Charge uniquement les rôles valides pour un utilisateur.
     */
    private Set<Role> loadValidRoles(UUID userId) {
        try {
            Query rolesQuery = entityManager.createNativeQuery(
                "SELECT r.id, r.name FROM roles r " +
                "JOIN user_roles ur ON ur.role_id = r.id " +
                "WHERE ur.user_id = :userId AND r.name IN ('SUPER_ADMIN', 'BUSINESS_OWNER')"
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
