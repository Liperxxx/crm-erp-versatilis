package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardSummaryResponse(
    Long companyId,
    String companyName,
    LocalDate start,
    LocalDate end,
    BigDecimal totalRevenue,
    BigDecimal totalExpense,
    BigDecimal profit,
    BigDecimal margin,
    BigDecimal cashBalance,
    BigDecimal overdueAmount,
    long pendingCount,
    long overdueCount
) {}