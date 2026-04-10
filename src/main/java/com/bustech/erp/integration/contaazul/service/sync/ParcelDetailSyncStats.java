package com.bustech.erp.integration.contaazul.service.sync;

/**
 * Aggregate counts from a full parcel-detail sync run executed by
 * {@link ParcelDetailSyncService#syncAllParcelAllocations}.
 *
 * @param parcelsProcessed      number of parcelas for which the detail endpoint
 *                              was successfully called (regardless of rateio presence)
 * @param allocations           total category-level allocation rows created across
 *                              all parcelas ({@code financial_event_allocations})
 * @param allocationCostCenters total cost-center sub-allocation rows created across
 *                              all parcelas ({@code financial_event_allocation_cost_centers})
 */
public record ParcelDetailSyncStats(
    int parcelsProcessed,
    int allocations,
    int allocationCostCenters
) {

    public static ParcelDetailSyncStats zero() {
        return new ParcelDetailSyncStats(0, 0, 0);
    }
}
