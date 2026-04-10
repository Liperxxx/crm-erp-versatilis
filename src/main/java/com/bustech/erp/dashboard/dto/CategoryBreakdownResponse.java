package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record CategoryBreakdownResponse(
    List<CategoryBreakdownItem> income,
    List<CategoryBreakdownItem> expense,
    BigDecimal totalIncome,
    BigDecimal totalExpense
) {}