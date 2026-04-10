package com.bustech.erp.integration.contaazul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// File: integration/contaazul/dto/ContaAzulCostCenterDto.java
// Represents a cost center from the Conta Azul API.
// Adjust field names to match the real API response if needed.
public record ContaAzulCostCenterDto(
    String id,
    @JsonProperty("nome") String name
) {}
