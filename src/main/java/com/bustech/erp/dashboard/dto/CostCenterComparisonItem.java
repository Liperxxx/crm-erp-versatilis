package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record CostCenterComparisonItem(
    String costCenterName,
    BigDecimal amountA,
    BigDecimal amountB,
    /** (A − B) / |B| × 100 */
    BigDecimal diffPct
) {}
