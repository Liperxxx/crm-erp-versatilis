package com.bustech.erp.integration.contaazul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// File: integration/contaazul/dto/ContaAzulCategoryDto.java
// Represents a category from the Conta Azul API.
// Adjust field names to match the real API response if needed.
public record ContaAzulCategoryDto(
    String id,
    String name,
    String type  // expected: "INCOME" or "EXPENSE"
) {}
