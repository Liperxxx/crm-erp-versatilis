package com.bustech.erp.integration.contaazul.dto;

import java.time.Instant;
import java.util.List;

/**
 * Summary returned by the Conta Azul sync API after a full company sync run.
 *
 * <ul>
 *   <li>{@code success} — true if no step produced an error; false on partial failure</li>
 *   <li>{@code syncedCategories} — financial categories upserted from Conta Azul</li>
 *   <li>{@code syncedCostCenters} — cost centers upserted from Conta Azul</li>
 *   <li>{@code syncedAccounts} — financial accounts upserted from Conta Azul</li>
 *   <li>{@code syncedEvents} — financial events (parcelas) upserted from the list endpoint</li>
 *   <li>{@code syncedParcelDetails} — parcelas for which the detail endpoint was successfully called</li>
 *   <li>{@code syncedAllocations} — category-level rateio rows created
 *       ({@code financial_event_allocations})</li>
 *   <li>{@code syncedAllocationCostCenters} — cost-center sub-rateio rows created
 *       ({@code financial_event_allocation_cost_centers})</li>
 *   <li>{@code errors} — list of step-level error messages (empty when {@code success = true})</li>
 *   <li>{@code syncedAt} — timestamp of sync completion</li>
 * </ul>
 */
public record ContaAzulSyncResult(
    Long companyId,
    boolean success,
    int syncedCategories,
    int syncedCostCenters,
    int syncedAccounts,
    int syncedEvents,
    int syncedParcelDetails,
    int syncedAllocations,
    int syncedAllocationCostCenters,
    List<String> errors,
    Instant syncedAt
) {
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public int totalEntitySynced() {
        return syncedCategories + syncedCostCenters + syncedAccounts + syncedEvents;
    }
}
