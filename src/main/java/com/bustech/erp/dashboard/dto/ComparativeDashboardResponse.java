package com.bustech.erp.dashboard.dto;

import java.util.List;

public record ComparativeDashboardResponse(
    List<CompanySummaryDto> companies,
    ConsolidatedSummaryDto consolidated
) {}
