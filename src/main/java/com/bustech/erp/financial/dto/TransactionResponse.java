package com.bustech.erp.financial.dto;

import com.bustech.erp.common.enums.TransactionStatus;
import com.bustech.erp.common.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(
    Long id,
    Long companyId,
    String companyName,
    TransactionType type,
    TransactionStatus status,
    String description,
    BigDecimal amount,
    BigDecimal paidAmount,
    LocalDate dueDate,
    LocalDate paymentDate,
    String category,
    String costCenter,
    String notes,
    String externalSource,
    Instant createdAt
) {}
