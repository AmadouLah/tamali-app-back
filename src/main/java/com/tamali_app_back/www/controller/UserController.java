package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.UserDto;
import com.tamali_app_back.www.dto.request.ChangeTemporaryPasswordRequest;
import com.tamali_app_back.www.dto.request.ConfirmCodeRequest;
import com.tamali_app_back.www.dto.request.CreateAssociateRequest;
import com.tamali_app_back.www.dto.request.CreateBusinessOwnerRequest;
import com.tamali_app_back.www.dto.request.UserCreateRequest;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.service.UserService;
import com.tamali_app_back.www.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable UUID id) {
        UserDto dto = ResponseUtil.requireFound(userService.getById(id), "Utilisateur", id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping(params = "email")
    public ResponseEntity<UserDto> findByEmail(@RequestParam String email) {
        UserDto dto = ResponseUtil.requireFound(userService.findByEmail(email), "Utilisateur", email);
        return ResponseEntity.ok(dto);
    }

    /**
     * Récupère tous les propriétaires d'entreprise.
     */
    @GetMapping("/business-owners")
    public ResponseEntity<List<UserDto>> findAllBusinessOwners() {
        return ResponseEntity.ok(userService.findAllBusinessOwners());
    }

    /**
     * Récupère tous les associés d'une entreprise.
     */
    @GetMapping("/business/{businessId}/associates")
    public ResponseEntity<List<UserDto>> findAssociatesByBusinessId(@PathVariable UUID businessId) {
        return ResponseEntity.ok(userService.findAssociatesByBusinessId(businessId));
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserCreateRequest request) {
        UserDto dto = userService.create(
                request.firstname(), request.lastname(), request.email(), request.password(),
                request.businessId(), request.roleTypes());
        if (dto == null) throw new BadRequestException("Cet email est déjà utilisé.");
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{id}/verification-code")
    public ResponseEntity<Void> setVerificationCode(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        String code = body.get("code");
        String expiration = body.get("expiration");
        if (code == null || expiration == null)
            throw new BadRequestException("Paramètres manquants : code et expiration requis.");
        LocalDateTime exp = LocalDateTime.parse(expiration);
        userService.setVerificationCode(id, code, exp);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm-code")
    public ResponseEntity<UserDto> confirmCode(@PathVariable UUID id, @Valid @RequestBody ConfirmCodeRequest request) {
        UserDto dto = userService.confirmCodeAndEnable(id, request.code());
        if (dto == null) throw new BadRequestException("Code invalide ou expiré.");
        return ResponseEntity.ok(dto);
    }

    /**
     * Crée un propriétaire d'entreprise avec juste l'email.
     * Génère un mot de passe temporaire et l'envoie par email.
     */
    @PostMapping("/business-owner")
    public ResponseEntity<UserDto> createBusinessOwner(@Valid @RequestBody CreateBusinessOwnerRequest request) {
        UserDto dto = userService.createBusinessOwnerWithEmail(request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Crée un associé pour un propriétaire d'entreprise.
     * Vérifie que le propriétaire a complété les 6 étapes de création de son entreprise.
     * Génère un mot de passe temporaire et l'envoie par email.
     */
    @PostMapping("/{ownerId}/associate")
    public ResponseEntity<UserDto> createAssociate(
            @PathVariable UUID ownerId,
            @Valid @RequestBody CreateAssociateRequest request) {
        UserDto dto = userService.createAssociateForOwner(ownerId, request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Change le mot de passe temporaire d'un utilisateur.
     */
    @PostMapping("/{id}/change-temporary-password")
    public ResponseEntity<UserDto> changeTemporaryPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeTemporaryPasswordRequest request) {
        UserDto dto = userService.changeTemporaryPassword(id, request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(dto);
    }

    /**
     * Met à jour un utilisateur (businessId, firstname, lastname).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> update(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> updates) {
        UUID businessId = updates.containsKey("businessId") && updates.get("businessId") != null
                ? UUID.fromString(updates.get("businessId").toString())
                : null;
        String firstname = updates.containsKey("firstname") ? (String) updates.get("firstname") : null;
        String lastname = updates.containsKey("lastname") ? (String) updates.get("lastname") : null;
        UserDto dto = userService.update(id, businessId, firstname, lastname);
        return ResponseEntity.ok(dto);
    }

    /**
     * Désactive le compte d'un utilisateur.
     */
    @PatchMapping("/{id}/disable")
    public ResponseEntity<UserDto> disableAccount(@PathVariable UUID id) {
        UserDto dto = userService.disableAccount(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Active le compte d'un utilisateur.
     */
    @PatchMapping("/{id}/enable")
    public ResponseEntity<UserDto> enableAccount(@PathVariable UUID id) {
        UserDto dto = userService.enableAccount(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Supprime définitivement le compte d'un utilisateur.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable UUID id) {
        userService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
