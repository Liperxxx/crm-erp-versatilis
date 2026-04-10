package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record ConsolidatedMonthlyResponse(
    List<ConsolidatedMonthlyPoint> points,
    BigDecimal revenueTotal,
    BigDecimal expensesTotal,
    BigDecimal profitTotal,
    List<CompanyRevenueShareDto> revenueShareByCompany
) {}