package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.InvitationDto;
import com.tamali_app_back.www.dto.UserDto;
import com.tamali_app_back.www.dto.request.AcceptInvitationRequest;
import com.tamali_app_back.www.dto.request.CreateInvitationRequest;
import com.tamali_app_back.www.entity.Invitation;
import com.tamali_app_back.www.entity.User;
import com.tamali_app_back.www.enums.RoleType;
import com.tamali_app_back.www.exception.BadRequestException;
import com.tamali_app_back.www.repository.UserRepository;
import com.tamali_app_back.www.service.EntityMapper;
import com.tamali_app_back.www.service.InvitationService;
import com.tamali_app_back.www.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
    private final EntityMapper mapper;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<InvitationDto> createInvitation(
            @Valid @RequestBody CreateInvitationRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Role") String userRole) {
        
        if (!RoleType.SUPER_ADMIN.name().equals(userRole)) {
            throw new BadRequestException("Seul le SUPER_ADMIN peut créer des invitations.");
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Utilisateur introuvable."));
        Invitation invitation = invitationService.createInvitation(request.email(), currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(invitation));
    }

    @GetMapping("/{token}")
    public ResponseEntity<InvitationDto> getByToken(@PathVariable String token) {
        Invitation invitation = invitationService.getByToken(token);
        return ResponseEntity.ok(mapper.toDto(invitation));
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<UserDto> acceptInvitation(
            @PathVariable String token,
            @Valid @RequestBody AcceptInvitationRequest request) {
        User user = invitationService.acceptInvitation(
                token, request.password(), request.firstname(), request.lastname());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(user));
    }
}
