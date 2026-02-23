package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.AnnouncementDto;
import com.tamali_app_back.www.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur public pour récupérer l'annonce active (page de connexion, dashboards).
 */
@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/current")
    public ResponseEntity<AnnouncementDto> getCurrent() {
        return announcementService.getCurrent()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
