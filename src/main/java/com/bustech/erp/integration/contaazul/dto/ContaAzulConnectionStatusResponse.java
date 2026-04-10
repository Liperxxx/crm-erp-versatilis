package com.bustech.erp.integration.contaazul.dto;

import com.bustech.erp.common.enums.ContaAzulConnectionStatus;

import java.time.Instant;

public record ContaAzulConnectionStatusResponse(
    Long companyId,
    ContaAzulConnectionStatus status,
    boolean connected,
    boolean tokenExpired,
    String externalCompanyName,
    String externalCompanyId,
    Instant expiresAt,
    Instant lastSyncAt
) {}
