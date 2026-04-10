package com.bustech.erp.dashboard.controller;

import com.bustech.erp.common.response.ApiResponse;
import com.bustech.erp.dashboard.dto.ConsolidatedMonthlyResponse;
import com.bustech.erp.dashboard.dto.ConsolidatedSummaryResponse;
import com.bustech.erp.dashboard.service.ConsolidatedDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard/consolidated")
@RequiredArgsConstructor
public class ConsolidatedDashboardController {

    private final ConsolidatedDashboardService consolidatedService;

    /**
     * Aggregated totals across all companies for the period.
     * Includes revenueTotal, expensesTotal, profitTotal, marginTotal,
     * and each company's revenue share percentage.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ConsolidatedSummaryResponse>> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(consolidatedService.getSummary(start, end)));
    }

    /**
     * Month-by-month consolidated results with per-company breakdown per point.
     * Includes period totals and revenue share by company.
     */
    @GetMapping("/monthly-results")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ConsolidatedMonthlyResponse>> monthlyResults(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(consolidatedService.getMonthlyResults(start, end)));
    }
}