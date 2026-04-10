package com.bustech.erp.dashboard.controller;

import com.bustech.erp.common.response.ApiResponse;
import com.bustech.erp.common.util.SecurityUtils;
import com.bustech.erp.dashboard.dto.ComparativeDashboardResponse;
import com.bustech.erp.dashboard.dto.DashboardResponse;
import com.bustech.erp.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard(
            @RequestParam(defaultValue = "0") int year) {
        Long companyId = SecurityUtils.getCurrentCompanyId();
        int targetYear = year == 0 ? LocalDate.now().getYear() : year;
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getDashboard(companyId, targetYear)));
    }

    @GetMapping("/comparative")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ComparativeDashboardResponse>> getComparative(
            @RequestParam(defaultValue = "0") int year) {
        int targetYear = year == 0 ? LocalDate.now().getYear() : year;
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getComparativeDashboard(targetYear)));
    }
}
