package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record CostCenterComparisonResponse(
    String companyA,
    String companyB,
    /** "INCOME" or "EXPENSE" */
    String direction,
    List<CostCenterComparisonItem> items,
    BigDecimal totalA,
    BigDecimal totalB,
    /** (totalA − totalB) / |totalB| × 100 */
    BigDecimal diffPct
) {}
