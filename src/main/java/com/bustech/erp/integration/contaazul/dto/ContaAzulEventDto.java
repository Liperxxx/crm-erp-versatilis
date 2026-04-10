package com.bustech.erp.integration.contaazul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// File: integration/contaazul/dto/ContaAzulEventDto.java
// Represents a financial event from the Conta Azul API.
// Adjust field names, type values, and status values to match the real API response.
public record ContaAzulEventDto(
    String id,
    String description,
    // expected values: "INCOME" or "EXPENSE" (adjust if API uses different terms)
    String type,
    BigDecimal amount,
    @JsonProperty("issue_date") LocalDate issueDate,
    @JsonProperty("due_date") LocalDate dueDate,
    @JsonProperty("paid_date") LocalDate paidDate,
    // expected values: PENDING, PAID, OVERDUE, CANCELLED (adjust if API uses different terms)
    String status,
    // Simple 1:1 FK fields — used when the API does not return rateio
    @JsonProperty("category_id") String categoryId,
    @JsonProperty("cost_center_id") String costCenterId,
    @JsonProperty("account_id") String accountId,
    // Full apportionment array — present when the API returns rateio[]
    @JsonProperty("rateio") List<ContaAzulAllocationDto> allocations
) {}
