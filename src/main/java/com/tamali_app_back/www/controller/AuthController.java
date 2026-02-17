package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.UserDto;
import com.tamali_app_back.www.dto.request.RequestCodeRequest;
import com.tamali_app_back.www.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * Demande de code de connexion à 6 chiffres. Envoi par email (Brevo).
     * Retourne les infos utilisateur (id, email) pour l'étape suivante (confirmation du code).
     */
    @PostMapping("/request-code")
    public ResponseEntity<UserDto> requestLoginCode(@Valid @RequestBody RequestCodeRequest request) {
        UserDto dto = userService.requestLoginCode(request.email());
        return ResponseEntity.ok(dto);
    }
}
