package com.bustech.erp.integration.contaazul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Represents one entry in the {@code rateio[]} array returned in the detail
 * of a Conta Azul financial event (parcela).
 *
 * <p>Each entry corresponds to one category slice. A category slice may itself
 * contain a list of cost-center sub-slices ({@code rateio_centro_custo[]}).
 *
 * <p>Adjust @JsonProperty values if the real API uses different field names.
 */
public record ContaAzulAllocationDto(
    @JsonProperty("id_categoria") String categoryId,
    @JsonProperty("nome_categoria") String categoryName,
    BigDecimal valor,
    @JsonProperty("rateio_centro_custo") List<ContaAzulAllocationCostCenterDto> costCenters
) {}
