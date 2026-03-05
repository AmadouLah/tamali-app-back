package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.BusinessActivityEntryDto;
import com.tamali_app_back.www.dto.SaleDto;
import com.tamali_app_back.www.dto.StockMovementDto;
import com.tamali_app_back.www.service.BusinessActivityExportService;
import com.tamali_app_back.www.service.SaleService;
import com.tamali_app_back.www.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses/{businessId}/exports")
@RequiredArgsConstructor
public class ExportController {

    private final SaleService saleService;
    private final StockMovementService stockMovementService;
    private final BusinessActivityExportService businessActivityExportService;

    @GetMapping("/sales")
    public ResponseEntity<List<SaleDto>> exportSales(
            @PathVariable UUID businessId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(saleService.exportSales(businessId, from, to));
    }

    @GetMapping("/stock-movements")
    public ResponseEntity<List<StockMovementDto>> exportStockMovements(
            @PathVariable UUID businessId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(stockMovementService.exportByBusinessId(businessId, from, to));
    }

    @GetMapping("/activity-log")
    public ResponseEntity<List<BusinessActivityEntryDto>> exportActivityLog(
            @PathVariable UUID businessId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        return ResponseEntity.ok(businessActivityExportService.exportActivity(businessId, from, to));
    }
}

