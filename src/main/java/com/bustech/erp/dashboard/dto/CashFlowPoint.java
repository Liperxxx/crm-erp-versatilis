package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record CashFlowPoint(
    int year,
    int month,
    String monthLabel,
    BigDecimal income,
    BigDecimal expense,
    BigDecimal net,
    BigDecimal cumulativeNet
) {}