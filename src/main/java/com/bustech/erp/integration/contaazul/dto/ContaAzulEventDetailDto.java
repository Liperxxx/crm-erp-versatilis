package com.bustech.erp.integration.contaazul.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Full detail DTO for a single Conta Azul parcela as returned by
 * {@code GET /v1/financeiro/eventos-financeiros/parcelas/{id}}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContaAzulEventDetailDto(

    String id,
    @JsonProperty("descricao") String description,
    @JsonProperty("valor_pago") BigDecimal amount,
    @JsonProperty("data_vencimento") LocalDate dueDate,
    String status,
    @JsonProperty("id_conta_financeira") String accountId,
    @JsonProperty("evento") EventoDto evento

) {
    /** Returns the rateio from the nested evento, if present. */
    public List<ContaAzulAllocationDto> allocations() {
        return (evento != null && evento.rateio() != null) ? evento.rateio() : List.of();
    }

    public boolean hasAllocations() {
        return !allocations().isEmpty();
    }

    public String type() {
        return (evento != null) ? evento.tipo() : null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventoDto(
        String id,
        @JsonProperty("tipo") String tipo,
        @JsonProperty("rateio") List<ContaAzulAllocationDto> rateio
    ) {}
}
