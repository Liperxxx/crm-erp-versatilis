package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ConsolidatedSummaryResponse(
    LocalDate start,
    LocalDate end,
    BigDecimal revenueTotal,
    BigDecimal expensesTotal,
    BigDecimal profitTotal,
    BigDecimal marginTotal,
    List<CompanyRevenueShareDto> revenueShareByCompany
) {}