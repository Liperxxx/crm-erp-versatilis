package com.bustech.erp.integration.contaazul.service;

import com.bustech.erp.common.enums.ContaAzulConnectionStatus;
import com.bustech.erp.company.repository.CompanyRepository;
import com.bustech.erp.integration.contaazul.client.ContaAzulDataClient;
import com.bustech.erp.integration.contaazul.dto.ContaAzulSyncResult;
import com.bustech.erp.integration.contaazul.entity.ContaAzulConnection;
import com.bustech.erp.integration.contaazul.repository.ContaAzulConnectionRepository;
import com.bustech.erp.integration.contaazul.service.sync.AccountSyncService;
import com.bustech.erp.integration.contaazul.service.sync.CategorySyncService;
import com.bustech.erp.integration.contaazul.service.sync.CostCenterSyncService;
import com.bustech.erp.integration.contaazul.service.sync.EventSyncService;
import com.bustech.erp.integration.contaazul.service.sync.ParcelDetailSyncService;
import com.bustech.erp.integration.contaazul.service.sync.ParcelDetailSyncStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

// File: integration/contaazul/service/ContaAzulSyncService.java
//
// Orchestrates the multi-step sync from Conta Azul into the local financial core.
// Steps are independent: a failure in one step is captured and reported
// without aborting subsequent steps.
@Slf4j
@Service
@RequiredArgsConstructor
public class ContaAzulSyncService {

    private final ContaAzulAuthService contaAzulAuthService;
    private final ContaAzulDataClient dataClient;
    private final ContaAzulConnectionRepository connectionRepository;
    private final CompanyRepository companyRepository;

    private final CategorySyncService categorySyncService;
    private final CostCenterSyncService costCenterSyncService;
    private final AccountSyncService accountSyncService;
    private final EventSyncService eventSyncService;
    private final ParcelDetailSyncService parcelDetailSyncService;

    /**
     * Syncs all financial data for a single company.
     * Steps: categories → cost centers → accounts → events.
     * Partial failures are captured in the result; lastSyncAt is always updated.
     */
    public ContaAzulSyncResult syncCompany(Long companyId) {
        log.info("[sync] Iniciando sincronizacao Conta Azul para empresa {}", companyId);

        ContaAzulConnection connection = contaAzulAuthService.getValidConnection(companyId);
        String accessToken = connection.getAccessToken();

        List<String> errors = new ArrayList<>();
        int categories = 0;
        int costCenters = 0;
        int accounts = 0;
        int events = 0;
        ParcelDetailSyncStats parcelStats = ParcelDetailSyncStats.zero();

        // Step 1: categories
        try {
            var dtos = dataClient.fetchCategories(accessToken);
            categories = categorySyncService.sync(companyId, dtos);
        } catch (Exception e) {
            log.error("[sync] Empresa {}: falha ao sincronizar categorias: {}", companyId, e.getMessage());
            errors.add("categories: " + e.getMessage());
        }

        // Step 2: cost centers
        try {
            var dtos = dataClient.fetchCostCenters(accessToken);
            costCenters = costCenterSyncService.sync(companyId, dtos);
        } catch (Exception e) {
            log.error("[sync] Empresa {}: falha ao sincronizar centros de custo: {}", companyId, e.getMessage());
            errors.add("cost-centers: " + e.getMessage());
        }

        // Step 3: accounts
        try {
            var dtos = dataClient.fetchAccounts(accessToken);
            accounts = accountSyncService.sync(companyId, dtos);
        } catch (Exception e) {
            log.error("[sync] Empresa {}: falha ao sincronizar contas: {}", companyId, e.getMessage());
            errors.add("accounts: " + e.getMessage());
        }

        // Step 4: events (depends on categories/cost-centers/accounts being present)
        try {
            var dtos = dataClient.fetchEvents(accessToken);
            events = eventSyncService.sync(companyId, dtos);
        } catch (Exception e) {
            log.error("[sync] Empresa {}: falha ao sincronizar eventos: {}", companyId, e.getMessage());
            errors.add("events: " + e.getMessage());
        }

        // Step 5: parcel detail — fetch rateio per event from the detail endpoint.
        // Runs even when step 4 had partial failures so existing events stay current.
        try {
            parcelStats = parcelDetailSyncService.syncAllParcelAllocations(companyId, accessToken);
        } catch (Exception e) {
            log.error("[sync] Empresa {}: falha ao sincronizar rateio das parcelas: {}", companyId, e.getMessage());
            errors.add("parcel-detail: " + e.getMessage());
        }

        // Always update lastSyncAt, even on partial failure
        connection.setLastSyncAt(Instant.now());
        connectionRepository.save(connection);

        boolean success = errors.isEmpty();
        ContaAzulSyncResult result = new ContaAzulSyncResult(
            companyId,
            success,
            categories,
            costCenters,
            accounts,
            events,
            parcelStats.parcelsProcessed(),
            parcelStats.allocations(),
            parcelStats.allocationCostCenters(),
            errors,
            Instant.now()
        );

        if (!success) {
            log.warn("[sync] Empresa {}: sincronizacao concluida com {} erro(s). " +
                "cat={} cc={} acc={} evt={} parcelas={} rateios={} ccRateios={}",
                companyId, errors.size(),
                categories, costCenters, accounts, events,
                parcelStats.parcelsProcessed(), parcelStats.allocations(),
                parcelStats.allocationCostCenters());
        } else {
            log.info("[sync] Empresa {}: sincronizacao concluida com sucesso. " +
                "cat={} cc={} acc={} evt={} parcelas={} rateios={} ccRateios={}",
                companyId,
                categories, costCenters, accounts, events,
                parcelStats.parcelsProcessed(), parcelStats.allocations(),
                parcelStats.allocationCostCenters());
        }

        return result;
    }

    /**
     * Syncs all companies that have an ACTIVE Conta Azul connection.
     * Failures per company are logged and do not abort other companies.
     */
    public void syncAll() {
        log.info("[sync-all] Iniciando sincronizacao global Conta Azul...");

        List<Long> activeCompanyIds = connectionRepository
            .findAllByStatusWithCompany(ContaAzulConnectionStatus.ACTIVE)
            .stream()
            .map(c -> c.getCompany().getId())
            .toList();

        log.info("[sync-all] {} empresa(s) com conexao ativa encontrada(s)", activeCompanyIds.size());

        int success = 0;
        int failed = 0;
        for (Long companyId : activeCompanyIds) {
            try {
                ContaAzulSyncResult result = syncCompany(companyId);
                if (result.hasErrors()) {
                    log.warn("[sync-all] Empresa {} sincronizada com erros parciais", companyId);
                }
                success++;
            } catch (Exception e) {
                log.error("[sync-all] Empresa {}: falha total na sincronizacao: {}", companyId, e.getMessage());
                failed++;
            }
        }

        log.info("[sync-all] Concluido. Sucesso: {}, Falhas: {}", success, failed);
    }
}
