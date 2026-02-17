package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.UserDto;
import com.tamali_app_back.www.dto.request.ConfirmCodeRequest;
import com.tamali_app_back.www.dto.request.UserCreateRequest;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.exception.ResourceNotFoundException;
import com.tamali_app_back.www.service.UserService;
import com.tamali_app_back.www.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
}
