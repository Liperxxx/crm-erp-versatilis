package com.bustech.erp.integration.contaazul.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Full detail DTO for a single Conta Azul financial event (parcela) as returned
 * by {@code GET /v1/financial-events/{id}}.
 *
 * <p>This is distinct from {@link ContaAzulEventDto} (which is used for the list
 * endpoint {@code GET /v1/financial-events}). The detail endpoint includes the
 * full {@code rateio[]} (apportionment) structure with category slices and
 * cost-center sub-slices.
 *
 * <p>Field name conventions follow the Conta Azul API response for financial events.
 * Adjust {@code @JsonProperty} values to match the real API if they differ.
 */
public record ContaAzulEventDetailDto(

    /** Unique ID of the financial event/parcela in Conta Azul. */
    String id,

    /** Human-readable description. */
    String description,

    /**
     * Event type/direction.
     * Expected values: {@code "INCOME"} / {@code "EXPENSE"}
     * (also: {@code "RECEITA"}, {@code "DESPESA"} — handled by mapping in sync service).
     */
    String type,

    /** Total amount of this parcela. */
    BigDecimal amount,

    /** Issue date (data de competência / emissão). */
    @JsonProperty("issue_date") LocalDate issueDate,

    /** Due date (data de vencimento). */
    @JsonProperty("due_date") LocalDate dueDate,

    /** Date the event was paid/received. Null if still pending. */
    @JsonProperty("paid_date") LocalDate paidDate,

    /**
     * Payment status.
     * Expected values: {@code "PENDING"}, {@code "PAID"}, {@code "OVERDUE"}, {@code "CANCELLED"}.
     */
    String status,

    /**
     * Primary single-category FK — populated when the event has no rateio.
     * When {@link #allocations} is non-empty, this field may be redundant.
     */
    @JsonProperty("category_id") String categoryId,

    /**
     * Primary single-cost-center FK — populated when the event has no rateio.
     * When {@link #allocations} is non-empty, this field may be redundant.
     */
    @JsonProperty("cost_center_id") String costCenterId,

    /** Payment account FK. */
    @JsonProperty("account_id") String accountId,

    /**
     * Full apportionment (rateio) array.
     * Each entry represents one category slice; each category slice may contain
     * multiple cost-center sub-slices.
     * Null or empty when the event has no rateio configured.
     */
    @JsonProperty("rateio") List<ContaAzulAllocationDto> allocations

) {
    /** Returns true if this parcela has at least one allocation entry. */
    public boolean hasAllocations() {
        return allocations != null && !allocations.isEmpty();
    }
}
