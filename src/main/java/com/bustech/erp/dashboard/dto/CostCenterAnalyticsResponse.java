package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Per-cost-center analytics breakdown for a single company.
 *
 * <p>Shared by the summary, revenue, and expenses variants of
 * {@code /api/dashboard/company/{id}/cost-centers/*}.
 */
public record CostCenterAnalyticsResponse(
    LocalDate start,
    LocalDate end,
    BigDecimal grandTotalRevenue,
    BigDecimal grandTotalExpenses,
    List<CostCenterAnalyticsItem> items
) {}
