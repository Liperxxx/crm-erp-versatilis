package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

/**
 * Per-category analytics item returned by all /categories/* endpoints.
 *
 * <ul>
 *   <li>{@code category} — category name; {@code "Sem Categoria"} when unresolved</li>
 *   <li>{@code totalRevenue} — total paid income allocated to this category in the period</li>
 *   <li>{@code totalExpenses} — total paid expenses allocated to this category in the period</li>
 *   <li>{@code totalProfit} — {@code totalRevenue - totalExpenses}</li>
 *   <li>{@code percentualSobreReceita} — this category's revenue as % of grand-total revenue</li>
 *   <li>{@code percentualSobreDespesa} — this category's expenses as % of grand-total expenses</li>
 * </ul>
 *
 * <p>Data source priority: rateio allocations ({@code financial_event_allocations}) are used
 * when present; events without any rateio fall back to the primary category FK
 * ({@code financial_events.category_id}).
 */
public record CategoryAnalyticsItem(
    String category,
    BigDecimal totalRevenue,
    BigDecimal totalExpenses,
    BigDecimal totalProfit,
    BigDecimal percentualSobreReceita,
    BigDecimal percentualSobreDespesa
) {}
