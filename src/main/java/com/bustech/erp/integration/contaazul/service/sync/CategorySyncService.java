package com.bustech.erp.integration.contaazul.service.sync;

import com.bustech.erp.common.enums.FinancialDirection;
import com.bustech.erp.company.service.CompanyService;
import com.bustech.erp.financial.entity.FinancialCategory;
import com.bustech.erp.financial.repository.FinancialCategoryRepository;
import com.bustech.erp.integration.contaazul.dto.ContaAzulCategoryDto;
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
public class CategorySyncService {

    private final FinancialCategoryRepository categoryRepository;
    private final CompanyService companyService;

    /**
     * Upserts all categories from the Conta Azul response for the given company.
     *
     * <p>After upserting, categories that were previously synced (externalId IS NOT NULL)
     * but are absent from the current response are marked {@code active = false}.
     * Manually-created categories (externalId IS NULL) are never deactivated.
     *
     * <p>If the API returns an empty list the method returns 0 without touching local data,
     * treating empty responses as a fetch failure rather than deliberate deletion.
     *
     * @return number of categories created or updated
     */
    @Transactional
    public int sync(Long companyId, List<ContaAzulCategoryDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            log.info("[cat-sync] Empresa {}: nenhuma categoria recebida — sync omitido", companyId);
            return 0;
        }

        var company = companyService.findById(companyId);
        Set<String> syncedExternalIds = new HashSet<>();
        int count = 0;

        for (ContaAzulCategoryDto dto : dtos) {
            if (dto.id() == null || dto.name() == null) {
                log.warn("[cat-sync] Empresa {}: categoria sem id/name ignorada (dto={})", companyId, dto);
                continue;
            }
            try {
                FinancialCategory entity = categoryRepository
                    .findByCompanyIdAndExternalId(companyId, dto.id())
                    .orElseGet(() -> FinancialCategory.builder()
                        .company(company)
                        .externalId(dto.id())
                        .build());

                entity.setName(dto.name());
                entity.setType(resolveDirection(dto.type()));
                entity.setActive(true);
                categoryRepository.save(entity);

                syncedExternalIds.add(dto.id());
                count++;
            } catch (Exception e) {
                log.error("[cat-sync] Empresa {}: erro ao salvar categoria externalId={}: {}",
                    companyId, dto.id(), e.getMessage());
                throw e;
            }
        }

        // Deactivate categories that were synced before but are no longer in the Conta Azul response.
        // Guard: only run when at least one category was successfully upserted, to avoid
        // accidentally deactivating everything when all DTOs were skipped due to bad data.
        if (!syncedExternalIds.isEmpty()) {
            int deactivated = categoryRepository.deactivateNotIn(companyId, syncedExternalIds);
            if (deactivated > 0) {
                log.info("[cat-sync] Empresa {}: {} categoria(s) desativada(s) — não presentes no Conta Azul",
                    companyId, deactivated);
            }
        }

        log.info("[cat-sync] Empresa {}: {} categoria(s) sincronizada(s)", companyId, count);
        return count;
    }

    private FinancialDirection resolveDirection(String type) {
        if (type == null) return FinancialDirection.EXPENSE;
        return switch (type.toUpperCase()) {
            case "INCOME", "RECEITA", "REVENUE", "CREDIT" -> FinancialDirection.INCOME;
            default -> FinancialDirection.EXPENSE;
        };
    }
}
