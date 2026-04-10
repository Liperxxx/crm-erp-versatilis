package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
    CompanySummaryDto companySummary,
    List<MonthlySeriesDto> monthlyIncome,
    List<MonthlySeriesDto> monthlyExpense
) {}
