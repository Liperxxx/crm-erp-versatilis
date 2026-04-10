package com.bustech.erp.integration.contaazul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

// File: integration/contaazul/dto/ContaAzulAccountDto.java
// Represents a financial account from the Conta Azul API.
// Adjust field names to match the real API response if needed.
public record ContaAzulAccountDto(
    String id,
    String name,
    @JsonProperty("current_balance") BigDecimal balance
) {}
