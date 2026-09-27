package com.ecommerce.exception.api.controller;

import com.ecommerce.exception.api.dto.DashboardSummaryDto;
import com.ecommerce.exception.service.ExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/backoffice/dashboard", "/api/v1/dashboard"})
@Tag(name = "Admin Dashboard", description = "Operational health and dashboard metric aggregation (API-DASH-001, FR-032)")
public class AdminDashboardController {

    private final ExceptionService exceptionService;

    public AdminDashboardController(ExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Operational dashboard summary metrics (API-DASH-001, FR-032)")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary() {
        DashboardSummaryDto summary = exceptionService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }
}
