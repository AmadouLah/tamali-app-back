package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.BusinessDto;
import com.tamali_app_back.www.dto.request.BusinessCreateRequest;
import com.tamali_app_back.www.dto.request.BusinessUpdateRequest;
import com.tamali_app_back.www.exception.ResourceNotFoundException;
import com.tamali_app_back.www.service.BusinessService;
import com.tamali_app_back.www.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    @GetMapping
    public ResponseEntity<List<BusinessDto>> findAll() {
        return ResponseEntity.ok(businessService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessDto> getById(@PathVariable UUID id) {
        BusinessDto dto = ResponseUtil.requireFound(businessService.getById(id), "Business", id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<BusinessDto> create(@Valid @RequestBody BusinessCreateRequest request) {
        BusinessDto dto = businessService.create(
                request.name(), request.email(), request.phone(), request.address());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BusinessDto> update(@PathVariable UUID id, @RequestBody BusinessUpdateRequest request) {
        BusinessDto dto = ResponseUtil.requireFound(businessService.update(id,
                request.name(), request.email(), request.phone(), request.address(), request.active()), "Business", id);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        businessService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
