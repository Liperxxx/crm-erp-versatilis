package com.bustech.erp.financial.dto;

import java.time.Instant;

public record CostCenterResponse(
    Long id,
    Long companyId,
    String externalId,
    String name,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {}
