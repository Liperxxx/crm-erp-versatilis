package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record MonthlySeriesDto(
    int year,
    int month,
    String monthLabel,
    BigDecimal amount
) {}
