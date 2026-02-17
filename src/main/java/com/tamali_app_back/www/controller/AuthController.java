package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.AuthResponse;
import com.tamali_app_back.www.dto.EmailCheckResponse;
import com.tamali_app_back.www.dto.UserDto;
import com.tamali_app_back.www.dto.request.CheckEmailRequest;
import com.tamali_app_back.www.dto.request.ConfirmLoginRequest;
import com.tamali_app_back.www.dto.request.PasswordLoginRequest;
import com.tamali_app_back.www.dto.request.RequestCodeRequest;
import com.tamali_app_back.www.entity.User;
import com.tamali_app_back.www.service.EntityMapper;
import com.tamali_app_back.www.service.JwtService;
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
    private final JwtService jwtService;
    private final EntityMapper mapper;

    /**
     * Vérifie si un email existe dans la base de données.
     */
    @PostMapping("/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmail(@Valid @RequestBody CheckEmailRequest request) {
        EmailCheckResponse response = userService.checkEmailExists(request.email());
        return ResponseEntity.ok(response);
    }

    /**
     * Authentification avec mot de passe. Envoie un code OTP par email.
     */
    @PostMapping("/login-password")
    public ResponseEntity<UserDto> loginWithPassword(@Valid @RequestBody PasswordLoginRequest request) {
        UserDto userDto = userService.authenticateWithPassword(request.userId(), request.password());
        return ResponseEntity.ok(userDto);
    }

    /**
     * Demande de code de connexion à 6 chiffres. Envoi par email (Brevo).
     * Retourne les infos utilisateur (id, email) pour l'étape suivante (confirmation du code).
     */
    @PostMapping("/request-code")
    public ResponseEntity<UserDto> requestLoginCode(@Valid @RequestBody RequestCodeRequest request) {
        UserDto dto = userService.requestLoginCode(request.email());
        return ResponseEntity.ok(dto);
    }

    /**
     * Confirmation du code de connexion. Retourne un token JWT et les informations utilisateur.
     */
    @PostMapping("/confirm-login")
    public ResponseEntity<AuthResponse> confirmLogin(@Valid @RequestBody ConfirmLoginRequest request) {
        User user = userService.confirmLogin(request.userId(), request.code());
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token, mapper.toDto(user)));
    }
}
