package com.bustech.erp.financial.dto;

import com.bustech.erp.common.enums.FinancialDirection;

import java.time.Instant;

public record FinancialCategoryResponse(
    Long id,
    Long companyId,
    String externalId,
    String name,
    FinancialDirection type,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
