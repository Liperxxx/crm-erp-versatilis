package com.bustech.erp.dashboard.controller;

import com.bustech.erp.common.response.ApiResponse;
import com.bustech.erp.dashboard.dto.*;
import com.bustech.erp.dashboard.service.ComparisonDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard/comparison")
@RequiredArgsConstructor
public class ComparisonDashboardController {

    private final ComparisonDashboardService comparisonService;

    /** Consolidated snapshot: totals, winners, and % differences for the period. */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<ComparisonSummaryResponse>> summary(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getSummary(companyA, companyB, start, end)));
    }

    /** Month-by-month revenue + expense + profit for both companies. */
    @GetMapping("/monthly-results")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<MonthlyResultsResponse>> monthlyResults(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getMonthlyResults(companyA, companyB, start, end)));
    }

    /** Month-by-month revenue comparison with % difference. Ready for dual-bar/line charts. */
    @GetMapping("/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<MonthlyComparisonResponse>> revenue(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getMonthlyRevenue(companyA, companyB, start, end)));
    }

    /** Month-by-month expenses comparison with % difference. */
    @GetMapping("/expenses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<MonthlyComparisonResponse>> expenses(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getMonthlyExpenses(companyA, companyB, start, end)));
    }

    /**
     * Month-by-month profit margin (%) comparison.
     * totalA/totalB in the response represent average margin % over the period.
     */
    @GetMapping("/margins")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<MonthlyComparisonResponse>> margins(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getMonthlyMargins(companyA, companyB, start, end)));
    }

    /**
     * Per-category breakdown comparison between two companies.
     * direction: INCOME | EXPENSE (default EXPENSE)
     */
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CategoryComparisonResponse>> categories(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam(defaultValue = "EXPENSE") String direction,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getCategoryComparison(companyA, companyB, direction, start, end)));
    }

    /**
     * Per-cost-center breakdown comparison between two companies.
     * direction: INCOME | EXPENSE (default EXPENSE)
     */
    @GetMapping("/cost-centers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CostCenterComparisonResponse>> costCenters(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam(defaultValue = "EXPENSE") String direction,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getCostCenterComparison(companyA, companyB, direction, start, end)));
    }

    // ─── Cost-Center Analytics Comparison (hybrid rateio-aware) ──────────────

    /**
     * Per-cost-center revenue comparison between two companies.
     *
     * <p>Uses rateio cost-center allocations when available; falls back to the event's
     * primary cost-center FK. Cost centers present in only one company appear with 0
     * for the other. Sorted by combined magnitude — ready for horizontal bar charts.
     */
    @GetMapping("/cost-centers/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CostCenterAnalyticsComparisonResponse>> costCenterAnalyticsRevenue(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getCostCenterAnalyticsRevenue(companyA, companyB, start, end)));
    }

    /**
     * Per-cost-center expense comparison between two companies.
     *
     * <p>Uses rateio cost-center allocations when available; falls back to the event's
     * primary cost-center FK. Cost centers present in only one company appear with 0.
     */
    @GetMapping("/cost-centers/expenses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CostCenterAnalyticsComparisonResponse>> costCenterAnalyticsExpenses(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getCostCenterAnalyticsExpenses(companyA, companyB, start, end)));
    }

    /**
     * Per-cost-center summary comparison between two companies.
     *
     * <p>Shows the revenue concentration by cost center for each company — ideal for
     * identifying which business units (Bustech vs Versatilis) drive which cost center.
     */
    @GetMapping("/cost-centers/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CostCenterAnalyticsComparisonResponse>> costCenterAnalyticsSummary(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getCostCenterAnalyticsSummary(companyA, companyB, start, end)));
    }

    // ─── Category Analytics Comparison (hybrid rateio-aware) ─────────────────

    /**
     * Per-category profit comparison between two companies.
     *
     * <p>Metric: profit = revenue − expenses per category.
     * Uses rateio allocations when available; falls back to the event's primary category FK.
     * Categories present in only one company appear with 0 for the other.
     * Sorted by combined magnitude descending — ready for horizontal bar charts.
     */
    @GetMapping("/categories/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CategoryAnalyticsComparisonResponse>> categoryAnalyticsSummary(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getCategoryAnalyticsSummary(companyA, companyB, start, end)));
    }

    /**
     * Per-category revenue comparison between two companies.
     *
     * <p>Uses rateio allocations when available; falls back to the event's primary category FK.
     * Categories present in only one company appear with 0 for the other.
     */
    @GetMapping("/categories/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CategoryAnalyticsComparisonResponse>> categoryAnalyticsRevenue(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getCategoryAnalyticsRevenue(companyA, companyB, start, end)));
    }

    /**
     * Per-category expense comparison between two companies.
     *
     * <p>Uses rateio allocations when available; falls back to the event's primary category FK.
     * Categories present in only one company appear with 0 for the other.
     */
    @GetMapping("/categories/expenses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CategoryAnalyticsComparisonResponse>> categoryAnalyticsExpenses(
            @RequestParam Long companyA,
            @RequestParam Long companyB,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(
            comparisonService.getCategoryAnalyticsExpenses(companyA, companyB, start, end)));
    }
}