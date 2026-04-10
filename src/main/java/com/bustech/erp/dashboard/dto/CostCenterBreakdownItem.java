package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record CostCenterBreakdownItem(
    String costCenterName,
    BigDecimal amount,
    BigDecimal percentage
) {}
