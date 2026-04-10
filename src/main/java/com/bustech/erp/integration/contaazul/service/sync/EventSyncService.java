package com.bustech.erp.integration.contaazul.service.sync;

import com.bustech.erp.common.enums.FinancialDirection;
import com.bustech.erp.common.enums.TransactionStatus;
import com.bustech.erp.company.service.CompanyService;
import com.bustech.erp.financial.entity.CostCenter;
import com.bustech.erp.financial.entity.FinancialAccount;
import com.bustech.erp.financial.entity.FinancialCategory;
import com.bustech.erp.financial.entity.FinancialEvent;
import com.bustech.erp.financial.repository.CostCenterRepository;
import com.bustech.erp.financial.repository.FinancialAccountRepository;
import com.bustech.erp.financial.repository.FinancialCategoryRepository;
import com.bustech.erp.financial.repository.FinancialEventRepository;
import com.bustech.erp.integration.contaazul.dto.ContaAzulEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// File: integration/contaazul/service/sync/EventSyncService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class EventSyncService {

    private final FinancialEventRepository eventRepository;
    private final FinancialCategoryRepository categoryRepository;
    private final CostCenterRepository costCenterRepository;
    private final FinancialAccountRepository accountRepository;
    private final CompanyService companyService;

    @Transactional
    public int sync(Long companyId, List<ContaAzulEventDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            log.info("[evt-sync] Empresa {}: nenhum evento recebido", companyId);
            return 0;
        }

        var company = companyService.findById(companyId);
        int count = 0;

        for (ContaAzulEventDto dto : dtos) {
            if (dto.id() == null || dto.amount() == null) {
                log.warn("[evt-sync] Empresa {}: evento sem id/amount ignorado", companyId);
                continue;
            }
            try {
                FinancialEvent entity = eventRepository
                    .findByCompanyIdAndExternalId(companyId, dto.id())
                    .orElseGet(() -> FinancialEvent.builder()
                        .company(company)
                        .externalId(dto.id())
                        .build());

                entity.setAmount(dto.amount());
                entity.setDescription(dto.description());
                entity.setDirection(resolveDirection(dto.type()));
                entity.setStatus(resolveStatus(dto.status()));
                entity.setIssueDate(dto.issueDate());
                entity.setDueDate(dto.dueDate());
                entity.setPaidDate(dto.paidDate());

                // Resolve nullable FKs by external IDs — missing FK doesn't block the event
                entity.setCategory(resolveCategory(companyId, dto.categoryId()));
                entity.setCostCenter(resolveCostCenter(companyId, dto.costCenterId()));
                entity.setFinancialAccount(resolveAccount(companyId, dto.accountId()));

                eventRepository.save(entity);
                count++;
            } catch (Exception e) {
                log.error("[evt-sync] Empresa {}: erro ao salvar evento externalId={}: {}",
                    companyId, dto.id(), e.getMessage());
                throw e;
            }
        }

        log.info("[evt-sync] Empresa {}: {} eventos sincronizados", companyId, count);
        return count;
    }

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

    private FinancialAccount resolveAccount(Long companyId, String externalId) {
        if (externalId == null) return null;
        return accountRepository.findByCompanyIdAndExternalId(companyId, externalId)
            .orElse(null);
    }

    private FinancialDirection resolveDirection(String type) {
        if (type == null) return FinancialDirection.EXPENSE;
        return switch (type.toUpperCase()) {
            case "INCOME", "RECEITA", "REVENUE", "CREDIT", "RECEIVABLE" -> FinancialDirection.INCOME;
            default -> FinancialDirection.EXPENSE;
        };
    }

    private TransactionStatus resolveStatus(String status) {
        if (status == null) return TransactionStatus.PENDING;
        return switch (status.toUpperCase()) {
            case "PAID", "RECEIVED", "LIQUIDADO", "PAGO" -> TransactionStatus.PAID;
            case "CANCELLED", "CANCELADO" -> TransactionStatus.CANCELLED;
            case "OVERDUE", "VENCIDO" -> TransactionStatus.OVERDUE;
            case "PARTIALLY_PAID", "PARCIALMENTE_PAGO" -> TransactionStatus.PARTIALLY_PAID;
            default -> TransactionStatus.PENDING;
        };
    }
}
