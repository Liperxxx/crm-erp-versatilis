package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ComparisonSummaryResponse(
    LocalDate start,
    LocalDate end,
    CompanySnapshotDto companyA,
    CompanySnapshotDto companyB,
    /** Company name with higher revenue */
    String higherRevenue,
    /** Company name with higher expense */
    String higherExpense,
    /** Company name with higher profit */
    String higherProfit,
    /** Company name with higher margin */
    String higherMargin,
    /** (A − B) / |B| × 100 — positive means A > B */
    BigDecimal revenueDiffPct,
    BigDecimal expenseDiffPct,
    BigDecimal profitDiffPct,
    BigDecimal marginDiffPct
) {}