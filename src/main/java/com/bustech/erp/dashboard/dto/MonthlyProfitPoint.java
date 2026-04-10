package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record MonthlyProfitPoint(
    int year,
    int month,
    String monthLabel,
    BigDecimal revenue,
    BigDecimal expense,
    BigDecimal profit
) {}