package com.bustech.erp.integration.contaazul.service.sync;

import com.bustech.erp.company.service.CompanyService;
import com.bustech.erp.financial.entity.CostCenter;
import com.bustech.erp.financial.repository.CostCenterRepository;
import com.bustech.erp.integration.contaazul.dto.ContaAzulCostCenterDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CostCenterSyncService {

    private final CostCenterRepository costCenterRepository;
    private final CompanyService companyService;

    /**
     * Upserts all cost centers from the Conta Azul response for the given company.
     *
     * <p>After upserting, cost centers that were previously synced (externalId IS NOT NULL)
     * but are absent from the current response are marked {@code active = false}.
     * Manually-created cost centers (externalId IS NULL) are never deactivated.
     *
     * <p>If the API returns an empty list the method returns 0 without touching local data,
     * treating empty responses as a fetch failure rather than deliberate deletion.
     *
     * @return number of cost centers created or updated
     */
    @Transactional
    public int sync(Long companyId, List<ContaAzulCostCenterDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            log.info("[cc-sync] Empresa {}: nenhum centro de custo recebido — sync omitido", companyId);
            return 0;
        }

        var company = companyService.findById(companyId);
        Set<String> syncedExternalIds = new HashSet<>();
        int count = 0;

        for (ContaAzulCostCenterDto dto : dtos) {
            if (dto.id() == null || dto.name() == null) {
                log.warn("[cc-sync] Empresa {}: centro de custo sem id/name ignorado (dto={})", companyId, dto);
                continue;
            }
            try {
                CostCenter entity = costCenterRepository
                    .findByCompanyIdAndExternalId(companyId, dto.id())
                    .orElseGet(() -> CostCenter.builder()
                        .company(company)
                        .externalId(dto.id())
                        .build());

                entity.setName(dto.name());
                entity.setActive(true);
                costCenterRepository.save(entity);

                syncedExternalIds.add(dto.id());
                count++;
            } catch (Exception e) {
                log.error("[cc-sync] Empresa {}: erro ao salvar cc externalId={}: {}",
                    companyId, dto.id(), e.getMessage());
                throw e;
            }
        }

        // Deactivate cost centers that were synced before but are no longer in the Conta Azul response.
        // Guard: only run when at least one cost center was successfully upserted.
        if (!syncedExternalIds.isEmpty()) {
            int deactivated = costCenterRepository.deactivateNotIn(companyId, syncedExternalIds);
            if (deactivated > 0) {
                log.info("[cc-sync] Empresa {}: {} centro(s) de custo desativado(s) — não presentes no Conta Azul",
                    companyId, deactivated);
            }
        }

        log.info("[cc-sync] Empresa {}: {} centro(s) de custo sincronizado(s)", companyId, count);
        return count;
    }
}
