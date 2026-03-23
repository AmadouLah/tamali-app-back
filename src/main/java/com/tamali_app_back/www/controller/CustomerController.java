package com.tamali_app_back.www.controller;

import com.tamali_app_back.www.dto.CustomerDetailsDto;
import com.tamali_app_back.www.dto.CustomerDto;
import com.tamali_app_back.www.dto.CustomerSummaryDto;
import com.tamali_app_back.www.dto.request.CustomerUpdateRequest;
import com.tamali_app_back.www.service.CustomerService;
import com.tamali_app_back.www.util.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/businesses/{businessId}/customers")
    public ResponseEntity<List<CustomerSummaryDto>> findByBusinessId(@PathVariable UUID businessId) {
        return ResponseEntity.ok(customerService.findSummariesByBusinessId(businessId));
    }

    @GetMapping("/businesses/{businessId}/customers/search")
    public ResponseEntity<List<CustomerDto>> search(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(customerService.searchByName(businessId, q));
    }

    @GetMapping("/businesses/{businessId}/customers/{customerId}")
    public ResponseEntity<CustomerDetailsDto> getDetails(
            @PathVariable UUID businessId,
            @PathVariable UUID customerId) {
        CustomerDetailsDto dto = ResponseUtil.requireFound(
                customerService.getDetails(businessId, customerId), "Client", customerId);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/businesses/{businessId}/customers/{customerId}")
    public ResponseEntity<CustomerDto> update(
            @PathVariable UUID businessId,
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerUpdateRequest request) {
        CustomerDto dto = ResponseUtil.requireFound(
                customerService.update(businessId, customerId, request.name(), request.phone()),
                "Client",
                customerId
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/businesses/{businessId}/customers/{customerId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID businessId,
            @PathVariable UUID customerId) {
        ResponseUtil.requireFound(
                customerService.deleteWithSales(businessId, customerId) ? Boolean.TRUE : null,
                "Client",
                customerId
        );
        return ResponseEntity.noContent().build();
    }
}
