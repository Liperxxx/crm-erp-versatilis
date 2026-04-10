package com.bustech.erp.integration.contaazul.service.sync;

/**
 * Immutable counts produced by a single call to
 * {@link AllocationSyncService#sync} for one financial event.
 *
 * @param allocations           category-level allocation rows created
 *                              ({@code financial_event_allocations})
 * @param costCenterAllocations cost-center sub-allocation rows created
 *                              ({@code financial_event_allocation_cost_centers})
 */
public record AllocationSyncStats(int allocations, int costCenterAllocations) {

    public static AllocationSyncStats empty() {
        return new AllocationSyncStats(0, 0);
    }

    public AllocationSyncStats add(AllocationSyncStats other) {
        return new AllocationSyncStats(
            this.allocations + other.allocations,
            this.costCenterAllocations + other.costCenterAllocations
        );
    }
}
