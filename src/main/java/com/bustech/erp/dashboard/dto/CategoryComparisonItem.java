package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record CategoryComparisonItem(
    String categoryName,
    BigDecimal amountA,
    BigDecimal amountB,
    /** (A − B) / |B| × 100 */
    BigDecimal diffPct
) {}
