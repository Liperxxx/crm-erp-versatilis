package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

public record ConsolidatedMonthlyPoint(
    int year,
    int month,
    String monthLabel,
    BigDecimal revenue,
    BigDecimal expense,
    BigDecimal profit,
    List<CompanyMonthEntryDto> byCompany
) {}