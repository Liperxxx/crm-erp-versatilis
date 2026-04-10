package com.bustech.erp.dashboard.controller;

import com.bustech.erp.common.response.ApiResponse;
import com.bustech.erp.dashboard.dto.*;
import com.bustech.erp.dashboard.service.CompanyDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard/company/{companyId}")
@RequiredArgsConstructor
public class CompanyDashboardController {

    private final CompanyDashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long costCenterId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getSummary(companyId, start, end, categoryId, costCenterId)));
    }

    @GetMapping("/monthly-revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<MonthlySeriesPoint>>> monthlyRevenue(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long costCenterId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getMonthlyRevenue(companyId, start, end, categoryId, costCenterId)));
    }

    @GetMapping("/monthly-expenses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<MonthlySeriesPoint>>> monthlyExpenses(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long costCenterId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getMonthlyExpenses(companyId, start, end, categoryId, costCenterId)));
    }

    @GetMapping("/monthly-profit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<MonthlyProfitPoint>>> monthlyProfit(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long costCenterId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getMonthlyProfit(companyId, start, end, categoryId, costCenterId)));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CategoryBreakdownResponse>> categories(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCategoryBreakdown(companyId, start, end)));
    }

    @GetMapping("/cost-centers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CostCenterBreakdownResponse>> costCenters(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCostCenterBreakdown(companyId, start, end)));
    }

    // ─── Category Analytics ───────────────────────────────────────────────────

    /**
     * Full per-category dashboard: revenue + expenses + profit + percentuals.
     * Data source: rateio allocations when present; event category FK as fallback.
     * Sorted by totalRevenue descending.
     *
     * @param costCenterId optional — narrow to events belonging to this cost center
     */
    @GetMapping("/categories/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CategoryAnalyticsResponse>> categoryAnalyticsSummary(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long costCenterId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCategoryAnalyticsSummary(companyId, start, end, costCenterId)));
    }

    /**
     * Revenue breakdown by category, sorted by totalRevenue descending.
     * Only income-direction paid events are considered.
     *
     * @param costCenterId optional — narrow to events belonging to this cost center
     */
    @GetMapping("/categories/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CategoryAnalyticsResponse>> categoryRevenue(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long costCenterId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCategoryRevenue(companyId, start, end, costCenterId)));
    }

    /**
     * Expenses breakdown by category, sorted by totalExpenses descending.
     * Only expense-direction paid events are considered.
     *
     * @param costCenterId optional — narrow to events belonging to this cost center
     */
    @GetMapping("/categories/expenses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CategoryAnalyticsResponse>> categoryExpenses(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long costCenterId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCategoryExpenses(companyId, start, end, costCenterId)));
    }

    /**
     * Per-category profit (revenue minus expenses), sorted by totalProfit descending.
     * Uses both income and expense paid events.
     *
     * @param costCenterId optional — narrow to events belonging to this cost center
     */
    @GetMapping("/categories/profit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CategoryAnalyticsResponse>> categoryProfit(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long costCenterId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCategoryProfit(companyId, start, end, costCenterId)));
    }

    @GetMapping("/cash-flow")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CashFlowResponse>> cashFlow(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long costCenterId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCashFlow(companyId, start, end, categoryId, costCenterId)));
    }

    // ─── Cost-Center Analytics ────────────────────────────────────────────────

    /**
     * Full per-cost-center dashboard: revenue + expenses + percentuals.
     * Uses rateio cost-center allocations when present; event cost-center FK as fallback.
     * Sorted by totalRevenue descending.
     *
     * @param categoryId optional — narrow to events belonging to this category
     */
    @GetMapping("/cost-centers/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CostCenterAnalyticsResponse>> costCenterAnalyticsSummary(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCostCenterAnalyticsSummary(companyId, start, end, categoryId)));
    }

    /**
     * Revenue breakdown by cost center, sorted by totalRevenue descending.
     *
     * @param categoryId optional — narrow to events belonging to this category
     */
    @GetMapping("/cost-centers/revenue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CostCenterAnalyticsResponse>> costCenterRevenue(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCostCenterRevenue(companyId, start, end, categoryId)));
    }

    /**
     * Expenses breakdown by cost center, sorted by totalExpenses descending.
     *
     * @param categoryId optional — narrow to events belonging to this category
     */
    @GetMapping("/cost-centers/expenses")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CostCenterAnalyticsResponse>> costCenterExpenses(
            @PathVariable Long companyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getCostCenterExpenses(companyId, start, end, categoryId)));
    }
}