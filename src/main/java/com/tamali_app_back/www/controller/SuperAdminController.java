package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.AnnouncementDto;
import com.tamali_app_back.www.dto.BusinessSummaryDto;
import com.tamali_app_back.www.dto.SuperAdminDashboardDto;
import com.tamali_app_back.www.dto.request.AnnouncementRequest;
import com.tamali_app_back.www.dto.request.BroadcastEmailRequest;
import com.tamali_app_back.www.service.AnnouncementService;
import com.tamali_app_back.www.service.SuperAdminService;
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
}
