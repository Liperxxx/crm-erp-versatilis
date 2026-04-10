package com.bustech.erp.integration.contaazul.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Generic paginated response wrapper from the Conta Azul API v2.
 * Some endpoints use {@code "itens"} and others {@code "items"} for the array field.
 * This DTO accepts both via separate {@code @JsonProperty} bindings merged into a single list.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContaAzulPageResponse<T>(
    @JsonProperty("itens_totais") int itensTotais,
    @JsonProperty("itens") List<T> itens,
    @JsonProperty("items") List<T> items
) {
    /** Returns whichever list is populated (itens or items), preferring itens. */
    public List<T> content() {
        if (itens != null && !itens.isEmpty()) return itens;
        if (items != null && !items.isEmpty()) return items;
        return List.of();
    }
}
