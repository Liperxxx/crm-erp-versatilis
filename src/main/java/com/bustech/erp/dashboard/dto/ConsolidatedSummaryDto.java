package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record ConsolidatedSummaryDto(
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal netResult,
    BigDecimal totalOverdue
) {}
