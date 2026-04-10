package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record MonthlySeriesPoint(
    int year,
    int month,
    String monthLabel,
    BigDecimal amount
) {}