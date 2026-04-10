package com.bustech.erp.dashboard.dto;

import java.math.BigDecimal;

public record CompanySnapshotDto(
    Long companyId,
    String companyName,
    BigDecimal revenue,
    BigDecimal expense,
    BigDecimal profit,
    BigDecimal margin
) {}