package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Monthly comparison series for revenue or expenses (summed totals).
 * For the /margins endpoint, totalA/totalB represent average margin %, not a sum.
 */
public record MonthlyComparisonResponse(
    String companyAName,
    String companyBName,
    List<MonthlyComparisonPoint> points,
    BigDecimal totalA,
    BigDecimal totalB,
    BigDecimal totalDiffPct
) {}