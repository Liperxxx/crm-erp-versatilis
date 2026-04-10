package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record CompanySummaryDto(
    Long companyId,
    String companyName,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal netResult,
    BigDecimal overdueAmount,
    long pendingCount,
    long overdueCount
) {}
