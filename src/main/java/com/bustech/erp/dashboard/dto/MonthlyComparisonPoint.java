package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record MonthlyComparisonPoint(
    int year,
    int month,
    String monthLabel,
    BigDecimal valueA,
    BigDecimal valueB,
    /** (A − B) / |B| × 100 */
    BigDecimal diffPct
) {}