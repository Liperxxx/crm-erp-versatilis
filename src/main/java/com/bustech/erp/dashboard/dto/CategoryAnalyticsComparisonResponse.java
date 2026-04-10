package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response for per-category comparison between two companies.
 *
 * <p>{@code metric} is one of {@code "REVENUE"}, {@code "EXPENSE"}, or {@code "PROFIT"},
 * indicating what {@code companyAValue} / {@code companyBValue} represent in each item.
 *
 * <p>{@code totalA} and {@code totalB} are the grand sums across all categories for each company.
 * {@code difference} and {@code differencePercent} describe the overall period comparison.
 *
 * <p>Items are sorted by the combined magnitude (|A| + |B|) so the most significant
 * categories appear first — ideal for horizontal bar charts and ranked comparisons.
 */
public record CategoryAnalyticsComparisonResponse(
    LocalDate start,
    LocalDate end,
    String companyA,
    String companyB,
    /** REVENUE | EXPENSE | PROFIT */
    String metric,
    List<CategoryAnalyticsComparisonItem> items,
    BigDecimal totalA,
    BigDecimal totalB,
    /** totalA − totalB */
    BigDecimal difference,
    /** (totalA − totalB) / |totalB| × 100 */
    BigDecimal differencePercent
) {}
