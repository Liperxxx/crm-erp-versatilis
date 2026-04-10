package com.bustech.erp.integration.contaazul.service.sync;

import com.bustech.erp.company.service.CompanyService;
import com.bustech.erp.financial.entity.FinancialAccount;
import com.bustech.erp.financial.repository.FinancialAccountRepository;
import com.bustech.erp.integration.contaazul.dto.ContaAzulAccountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

// File: integration/contaazul/service/sync/AccountSyncService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSyncService {

    private final FinancialAccountRepository accountRepository;
    private final CompanyService companyService;

    @Transactional
    public int sync(Long companyId, List<ContaAzulAccountDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            log.info("[acc-sync] Empresa {}: nenhuma conta recebida", companyId);
            return 0;
        }

        var company = companyService.findById(companyId);
        int count = 0;

        for (ContaAzulAccountDto dto : dtos) {
            if (dto.id() == null || dto.name() == null) {
                log.warn("[acc-sync] Empresa {}: conta sem id/name ignorada", companyId);
                continue;
            }
            try {
                FinancialAccount entity = accountRepository
                    .findByCompanyIdAndExternalId(companyId, dto.id())
                    .orElseGet(() -> FinancialAccount.builder()
                        .company(company)
                        .externalId(dto.id())
                        .build());

                entity.setName(dto.name());
                entity.setBalance(dto.balance() != null ? dto.balance() : BigDecimal.ZERO);
                accountRepository.save(entity);
                count++;
            } catch (Exception e) {
                log.error("[acc-sync] Empresa {}: erro ao salvar conta externalId={}: {}",
                    companyId, dto.id(), e.getMessage());
                throw e;
            }
        }

        log.info("[acc-sync] Empresa {}: {} contas sincronizadas", companyId, count);
        return count;
    }
}
