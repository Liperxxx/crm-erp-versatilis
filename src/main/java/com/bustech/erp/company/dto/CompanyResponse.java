package com.bustech.erp.company.dto;

import java.time.Instant;

public record CompanyResponse(
        Long id,
        String name,
        String slug,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
