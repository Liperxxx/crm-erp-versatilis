package com.bustech.erp.integration.contaazul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents one entry in the {@code rateio_centro_custo[]} sub-array of a
 * Conta Azul category allocation.
 *
 * <p>Field names follow the Conta Azul API response structure.
 * Adjust @JsonProperty values if the real API uses different keys.
 */
public record ContaAzulAllocationCostCenterDto(
    @JsonProperty("id_centro_custo") String id,
    @JsonProperty("nome_centro_custo") String name,
    BigDecimal valor
) {}
