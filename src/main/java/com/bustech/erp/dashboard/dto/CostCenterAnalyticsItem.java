package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

/**
 * One row in a per-cost-center analytics breakdown for a single company.
 *
 * <p>Data is sourced from rateio allocations when available, and from the
 * event's primary cost-center FK for events without rateio.
 */
public record CostCenterAnalyticsItem(
    String costCenterName,
    BigDecimal totalRevenue,
    BigDecimal totalExpenses,
    /** totalRevenue share as % of grand revenue; 0 when no revenue in period. */
    BigDecimal percentualSobreReceita,
    /** totalExpenses share as % of grand expenses; 0 when no expenses in period. */
    BigDecimal percentualSobreDespesa
) {}
