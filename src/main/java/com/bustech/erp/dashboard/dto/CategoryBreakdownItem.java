package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record CategoryBreakdownItem(
    String categoryName,
    BigDecimal amount,
    BigDecimal percentage
) {}