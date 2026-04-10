package com.bustech.erp.integration.contaazul.service.sync;

import com.bustech.erp.company.entity.Company;
import com.bustech.erp.company.service.CompanyService;
import com.bustech.erp.financial.entity.FinancialEvent;
import com.bustech.erp.financial.repository.FinancialEventRepository;
import com.bustech.erp.integration.contaazul.client.ContaAzulDataClient;
import com.bustech.erp.integration.contaazul.dto.ContaAzulEventDetailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Fetches the per-parcel detail from the Conta Azul API and syncs the
 * apportionment (rateio) into the local allocation tables.
 *
 * <h2>Responsibility</h2>
 * <p>This service is the <em>sole owner</em> of allocation persistence.
 * It is called <strong>after</strong> {@link EventSyncService} has already
 * saved basic event data. For each event with a known {@code externalId} it:
 * <ol>
 *   <li>Calls {@code GET /v1/financial-events/{id}} to get the full detail
 *       including {@code rateio[]}.</li>
 *   <li>Delegates to {@link AllocationSyncService} which clears and
 *       re-creates allocation rows atomically per event.</li>
 * </ol>
 *
 * <h2>Error handling</h2>
 * <ul>
 *   <li>A 404 for a single parcela is silently skipped (logged at WARN).</li>
 *   <li>Any non-404 HTTP error or unexpected exception for a single parcela
 *       is logged at ERROR and the loop continues — one failure never aborts
 *       other parcelas.</li>
 *   <li>The caller ({@link com.bustech.erp.integration.contaazul.service.ContaAzulSyncService})
 *       decides whether to surface partial errors in the sync result.</li>
 * </ul>
 *
 * <h2>Parcela sem rateio</h2>
 * <p>When the detail response has no {@code rateio} array (or it is empty),
 * {@link AllocationSyncService#sync} is still called so any stale allocation
 * rows from a previous sync are cleared.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParcelDetailSyncService {

    private final ContaAzulDataClient dataClient;
    private final FinancialEventRepository eventRepository;
    private final AllocationSyncService allocationSyncService;
    private final CompanyService companyService;

    /**
     * Fetches parcela detail for every event of the company that has a known
     * {@code externalId} and persists the resulting rateio.
     *
     * @param companyId   the owning company
     * @param accessToken valid OAuth2 access token for the company
     * @return aggregate stats: parcelas processed, allocation rows created,
     *         cost-center allocation rows created
     */
    public ParcelDetailSyncStats syncAllParcelAllocations(Long companyId, String accessToken) {

        Company company = companyService.findById(companyId);

        List<FinancialEvent> events =
            eventRepository.findByCompanyIdAndExternalIdIsNotNull(companyId);

        if (events.isEmpty()) {
            log.info("[parcel-detail] Empresa {}: nenhum evento com externalId encontrado — abortando",
                companyId);
            return ParcelDetailSyncStats.zero();
        }

        log.info("[parcel-detail] Empresa {}: buscando detalhe de {} parcela(s)...",
            companyId, events.size());

        int parcelsProcessed = 0;
        int notFound = 0;
        int errors = 0;
        AllocationSyncStats totals = AllocationSyncStats.empty();

        for (FinancialEvent event : events) {
            String externalId = event.getExternalId();
            try {
                Optional<ContaAzulEventDetailDto> detailOpt =
                    dataClient.fetchEventDetail(accessToken, externalId);

                if (detailOpt.isEmpty()) {
                    // 404 — parcela no longer exists in Conta Azul; clear stale allocations
                    notFound++;
                    allocationSyncService.sync(companyId, company, event, null);
                    continue;
                }

                ContaAzulEventDetailDto detail = detailOpt.get();

                if (detail.hasAllocations()) {
                    log.debug("[parcel-detail] Empresa {}: evento {}: {} entrada(s) de rateio",
                        companyId, externalId, detail.allocations().size());
                } else {
                    log.debug("[parcel-detail] Empresa {}: evento {}: sem rateio — limpando registros anteriores",
                        companyId, externalId);
                }

                AllocationSyncStats stats =
                    allocationSyncService.sync(companyId, company, event, detail.allocations());
                totals = totals.add(stats);
                parcelsProcessed++;

            } catch (Exception e) {
                errors++;
                log.error("[parcel-detail] Empresa {}: erro ao processar detalhe da parcela {}: {}",
                    companyId, externalId, e.getMessage());
                // Continue — one failure must not block the rest
            }
        }

        log.info(
            "[parcel-detail] Empresa {}: concluído. " +
            "Total={}, Processadas={}, NãoEncontradas={}, Erros={}, Rateios={}, CCRateios={}",
            companyId, events.size(), parcelsProcessed, notFound, errors,
            totals.allocations(), totals.costCenterAllocations());

        return new ParcelDetailSyncStats(
            parcelsProcessed,
            totals.allocations(),
            totals.costCenterAllocations()
        );
    }
}
