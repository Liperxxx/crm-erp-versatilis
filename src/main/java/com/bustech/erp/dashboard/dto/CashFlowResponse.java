package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record CashFlowResponse(
    List<CashFlowPoint> points,
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal totalNet
) {}