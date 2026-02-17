package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.BusinessSectorDto;
import com.tamali_app_back.www.dto.request.BusinessSectorCreateRequest;
import com.tamali_app_back.www.dto.request.BusinessSectorUpdateRequest;
import com.tamali_app_back.www.service.BusinessSectorService;
import com.tamali_app_back.www.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/business-sectors")
@RequiredArgsConstructor
public class BusinessSectorController {

    private final BusinessSectorService service;

    @GetMapping
    public ResponseEntity<List<BusinessSectorDto>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<BusinessSectorDto>> findActive() {
        return ResponseEntity.ok(service.findActive());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessSectorDto> getById(@PathVariable UUID id) {
        BusinessSectorDto dto = ResponseUtil.requireFound(service.getById(id), "Secteur d'activité", id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<BusinessSectorDto> create(@Valid @RequestBody BusinessSectorCreateRequest request) {
        BusinessSectorDto dto = service.create(request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BusinessSectorDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody BusinessSectorUpdateRequest request) {
        BusinessSectorDto dto = service.update(id, request.name(), request.description(), request.active());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
