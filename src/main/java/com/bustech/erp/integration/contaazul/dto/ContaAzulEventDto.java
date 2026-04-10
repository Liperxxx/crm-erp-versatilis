package com.bustech.erp.integration.contaazul.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents a financial event from the Conta Azul API v2.
 * Maps from both {@code /contas-a-receber/buscar} and {@code /contas-a-pagar/buscar}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContaAzulEventDto(
    String id,
    @JsonProperty("descricao") String description,
    /** Injected after fetch — not from API. RECEITA or DESPESA. */
    String type,
    @JsonProperty("total") BigDecimal amount,
    @JsonProperty("data_competencia") LocalDate issueDate,
    @JsonProperty("data_vencimento") LocalDate dueDate,
    @JsonProperty("status_traduzido") String status,
    /** First category id from the categorias array (convenience). */
    String categoryId,
    /** First cost center id from the centros_custo array (convenience). */
    String costCenterId,
    String accountId,
    @JsonProperty("categorias") List<CategoriaParcela> categorias,
    @JsonProperty("centros_custo") List<CentroCustoParcela> centrosCusto
) {
    /** Derive categoryId from the first element of categorias array. */
    public String categoryId() {
        if (categoryId != null) return categoryId;
        return (categorias != null && !categorias.isEmpty()) ? categorias.getFirst().id() : null;
    }
    /** Derive costCenterId from the first element of centros_custo array. */
    public String costCenterId() {
        if (costCenterId != null) return costCenterId;
        return (centrosCusto != null && !centrosCusto.isEmpty()) ? centrosCusto.getFirst().id() : null;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CategoriaParcela(String id, @JsonProperty("nome") String nome) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CentroCustoParcela(String id, @JsonProperty("nome") String nome) {}

    /** Create a copy with the type set (used to inject RECEITA/DESPESA after fetch). */
    public ContaAzulEventDto withType(String newType) {
        return new ContaAzulEventDto(id, description, newType, amount, issueDate, dueDate,
                status, categoryId, costCenterId, accountId, categorias, centrosCusto);
    }
}
