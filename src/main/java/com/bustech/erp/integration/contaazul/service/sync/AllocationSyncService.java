package com.bustech.erp.integration.contaazul.service.sync;

import com.bustech.erp.company.entity.Company;
import com.bustech.erp.financial.entity.*;
import com.bustech.erp.financial.repository.CostCenterRepository;
import com.bustech.erp.financial.repository.FinancialCategoryRepository;
import com.bustech.erp.financial.repository.FinancialEventAllocationRepository;
import com.bustech.erp.integration.contaazul.dto.ContaAzulAllocationCostCenterDto;
import com.bustech.erp.integration.contaazul.dto.ContaAzulAllocationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Syncs the apportionment (rateio) of a financial event into the local
 * {@code financial_event_allocations} and
 * {@code financial_event_allocation_cost_centers} tables.
 *
 * <p>Strategy: full replace per event. All existing allocations for the event
 * are deleted and re-created from the latest Conta Azul response.
 * This is safe because:
 * <ul>
 *   <li>Allocation rows are owned exclusively by their parent event (ON DELETE CASCADE).</li>
 *   <li>Nothing in the system references individual allocation rows by ID externally.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AllocationSyncService {

    private final FinancialEventAllocationRepository allocationRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final CostCenterRepository costCenterRepository;

    /**
     * Replaces all allocations for the given event with the data from the API.
     *
     * <p>Strategy: full replace — all existing allocation rows for the event are
     * deleted first, then re-created from the API response. The caller must ensure
     * this runs inside a transaction (or that the repository's own transaction
     * boundaries are sufficient).
     *
     * @param companyId the owning company
     * @param company   the Company entity (already loaded by the caller)
     * @param event     the FinancialEvent entity (already saved by the caller)
     * @param dtos      the {@code rateio[]} array from the Conta Azul response;
     *                  may be null or empty — allocations are then only cleared
     * @return counts of allocation rows and cost-center sub-allocation rows created
     */
    @Transactional
    public AllocationSyncStats sync(Long companyId, Company company, FinancialEvent event,
                                    List<ContaAzulAllocationDto> dtos) {

        // Always clear previous allocations; fresh data from API takes precedence
        allocationRepository.deleteByEventId(event.getId());

        if (dtos == null || dtos.isEmpty()) {
            log.debug("[alloc-sync] Empresa {}: evento {}: sem rateio — registros anteriores limpos",
                companyId, event.getExternalId());
            return AllocationSyncStats.empty();
        }

        int allocCount = 0;
        int ccCount = 0;

        for (ContaAzulAllocationDto dto : dtos) {
            if (dto.valor() == null) {
                log.warn("[alloc-sync] Empresa {}: evento {}: entrada de rateio sem valor ignorada (categoryId={})",
                    companyId, event.getExternalId(), dto.categoryId());
                continue;
            }

            try {
                FinancialCategory resolvedCategory = resolveCategory(companyId, dto.categoryId());

                FinancialEventAllocation allocation = FinancialEventAllocation.builder()
                    .company(company)
                    .event(event)
                    .category(resolvedCategory)
                    .externalCategoryId(dto.categoryId())
                    .categoryName(dto.categoryName())
                    .allocatedAmount(coerce(dto.valor()))
                    .build();

                // Populate cost-center sub-allocations before saving so JPA cascade handles them
                if (dto.costCenters() != null) {
                    for (ContaAzulAllocationCostCenterDto ccDto : dto.costCenters()) {
                        if (ccDto.valor() == null) continue;

                        CostCenter resolvedCc = resolveCostCenter(companyId, ccDto.id());

                        FinancialEventAllocationCostCenter ccAlloc =
                            FinancialEventAllocationCostCenter.builder()
                                .company(company)
                                .allocation(allocation)
                                .costCenter(resolvedCc)
                                .externalCostCenterId(ccDto.id())
                                .costCenterName(ccDto.name())
                                .allocatedAmount(coerce(ccDto.valor()))
                                .build();

                        allocation.getCostCenters().add(ccAlloc);
                        ccCount++;
                    }
                }

                allocationRepository.save(allocation);
                allocCount++;
            } catch (Exception e) {
                log.error("[alloc-sync] Empresa {}: evento {}: erro ao salvar rateio categoryId={}: {}",
                    companyId, event.getExternalId(), dto.categoryId(), e.getMessage());
                throw e;
            }
        }

        log.debug("[alloc-sync] Empresa {}: evento {}: {} rateio(s), {} centro(s) de custo salvos",
            companyId, event.getExternalId(), allocCount, ccCount);
        return new AllocationSyncStats(allocCount, ccCount);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private FinancialCategory resolveCategory(Long companyId, String externalId) {
        if (externalId == null) return null;
        return categoryRepository.findByCompanyIdAndExternalId(companyId, externalId)
            .orElse(null);
    }

    private CostCenter resolveCostCenter(Long companyId, String externalId) {
        if (externalId == null) return null;
        return costCenterRepository.findByCompanyIdAndExternalId(companyId, externalId)
            .orElse(null);
    }

    /** Guards against null amounts coming from unexpected API payloads. */
    private static BigDecimal coerce(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
