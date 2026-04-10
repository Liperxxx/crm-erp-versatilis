package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

/**
 * One row in a cost-center-level comparison between two companies.
 *
 * <p>Fields:
 * <ul>
 *   <li>{@code companyAValue} / {@code companyBValue} — the metric value (revenue, expense) for
 *       each company in that cost center during the requested period.
 *   <li>{@code difference} — companyAValue − companyBValue (positive means A > B).
 *   <li>{@code differencePercent} — (A − B) / |B| × 100; 0 when both are zero.
 * </ul>
 */
public record CostCenterAnalyticsComparisonItem(
    String costCenterName,
    BigDecimal companyAValue,
    BigDecimal companyBValue,
    BigDecimal difference,
    BigDecimal differencePercent
) {}
