package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.BusinessSummaryDto;
import com.tamali_app_back.www.dto.SuperAdminDashboardDto;
import com.tamali_app_back.www.service.SuperAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;

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
}
