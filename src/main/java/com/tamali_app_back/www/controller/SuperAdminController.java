package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.AnnouncementDto;
import com.tamali_app_back.www.dto.BusinessSummaryDto;
import com.tamali_app_back.www.dto.InstantNotificationSendResultDto;
import com.tamali_app_back.www.dto.NotificationUserOptionDto;
import com.tamali_app_back.www.dto.SuperAdminDashboardDto;
import com.tamali_app_back.www.dto.request.AnnouncementRequest;
import com.tamali_app_back.www.dto.request.BroadcastEmailRequest;
import com.tamali_app_back.www.dto.request.InstantNotificationRequest;
import com.tamali_app_back.www.dto.request.SuperAdminResetPasswordRequest;
import com.tamali_app_back.www.enums.RoleType;
import com.tamali_app_back.www.service.AnnouncementService;
import com.tamali_app_back.www.service.InstantNotificationService;
import com.tamali_app_back.www.service.SuperAdminService;
import com.tamali_app_back.www.service.UserService;
import com.tamali_app_back.www.exception.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final AnnouncementService announcementService;
    private final UserService userService;
    private final InstantNotificationService instantNotificationService;

    @GetMapping("/dashboard")
    public ResponseEntity<SuperAdminDashboardDto> getDashboard() {
        SuperAdminDashboardDto dto = new SuperAdminDashboardDto(
                superAdminService.getPlatformStats(),
                superAdminService.getRecentActivity(),
                superAdminService.getUsageStats(),
                superAdminService.getSystemMonitoring());
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/businesses")
    public ResponseEntity<List<BusinessSummaryDto>> getBusinessSummaries() {
        return ResponseEntity.ok(superAdminService.getBusinessSummaries());
    }

    @PostMapping("/announcements")
    public ResponseEntity<AnnouncementDto> setAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        return ResponseEntity.ok(announcementService.setCurrent(request.message()));
    }

    @DeleteMapping("/announcements/current")
    public ResponseEntity<Void> clearAnnouncement() {
        announcementService.clearCurrent();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/announcements/broadcast-email")
    public ResponseEntity<Void> broadcastEmail(@Valid @RequestBody BroadcastEmailRequest request) {
        announcementService.broadcastEmail(request.subject(), request.message());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/notification-options")
    public ResponseEntity<List<NotificationUserOptionDto>> notificationUserOptions(
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        requireSuperAdmin(userRole);
        return ResponseEntity.ok(userService.findAllForNotificationOptions());
    }

    @PostMapping("/notifications/instant")
    public ResponseEntity<InstantNotificationSendResultDto> sendInstantNotification(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody InstantNotificationRequest request
    ) {
        return ResponseEntity.ok(instantNotificationService.sendAsSuperAdmin(userRole, request));
    }

    /**
     * Réinitialise le mot de passe d'un utilisateur existant et envoie un mot de passe temporaire par email.
     * Ne supprime jamais les données de l'utilisateur : seule la valeur du mot de passe est remplacée.
     *
     * Sécurité: réservé au SUPER_ADMIN via header X-User-Role.
     */
    @PostMapping("/users/reset-password")
    public ResponseEntity<Void> resetUserPassword(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody SuperAdminResetPasswordRequest request
    ) {
        requireSuperAdmin(userRole);
        userService.resetPasswordAsSuperAdmin(request.email());
        return ResponseEntity.noContent().build();
    }

    private static void requireSuperAdmin(String userRole) {
        if (!RoleType.SUPER_ADMIN.name().equals(userRole)) {
            throw new BadRequestException("Seul le SUPER_ADMIN peut effectuer cette action.");
        }
    }
}
