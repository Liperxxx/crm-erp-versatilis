package com.bustech.erp.company.dto;

import com.bustech.erp.common.enums.UserRole;

import java.time.Instant;

public record UserResponse(
    Long id,
    String name,
    String email,
    UserRole role,
    boolean active,
    Long companyId,
    String companyName,
    Instant createdAt
) {}
