package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record CostCenterBreakdownResponse(
    List<CostCenterBreakdownItem> income,
    List<CostCenterBreakdownItem> expense,
    BigDecimal totalIncome,
    BigDecimal totalExpense
) {}
