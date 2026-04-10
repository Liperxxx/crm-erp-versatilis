package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response for per-cost-center comparison between two companies.
 *
 * <p>{@code metric} is one of {@code "REVENUE"} or {@code "EXPENSE"}, indicating what
 * {@code companyAValue} / {@code companyBValue} represent in each item.
 *
 * <p>Cost centers that exist for only one company appear with 0 for the other.
 * Items are sorted by combined magnitude (|A| + |B|) descending — ideal for bar charts.
 */
public record CostCenterAnalyticsComparisonResponse(
    LocalDate start,
    LocalDate end,
    String companyA,
    String companyB,
    /** REVENUE | EXPENSE */
    String metric,
    List<CostCenterAnalyticsComparisonItem> items,
    BigDecimal totalA,
    BigDecimal totalB,
    /** totalA − totalB */
    BigDecimal difference,
    /** (totalA − totalB) / |totalB| × 100 */
    BigDecimal differencePercent
) {}
