package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record CompanyRevenueShareDto(
    Long companyId,
    String companyName,
    BigDecimal revenue,
    BigDecimal sharePercent
) {}