package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response payload for all {@code /categories/*} dashboard endpoints.
 *
 * <p>The {@code items} list is sorted by the field most relevant to the
 * specific endpoint (see individual endpoint documentation):
 * <ul>
 *   <li>{@code /summary}   — sorted by {@code totalRevenue} descending</li>
 *   <li>{@code /revenue}   — sorted by {@code totalRevenue} descending</li>
 *   <li>{@code /expenses}  — sorted by {@code totalExpenses} descending</li>
 *   <li>{@code /profit}    — sorted by {@code totalProfit} descending</li>
 * </ul>
 *
 * <p>Categories with both zero revenue and zero expenses are excluded.
 */
public record CategoryAnalyticsResponse(
    LocalDate start,
    LocalDate end,
    BigDecimal grandTotalRevenue,
    BigDecimal grandTotalExpenses,
    BigDecimal grandTotalProfit,
    List<CategoryAnalyticsItem> items
) {}
