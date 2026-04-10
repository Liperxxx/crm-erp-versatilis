package com.bustech.erp.financial.service;

import com.bustech.erp.common.enums.FinancialDirection;
import com.bustech.erp.common.enums.TransactionStatus;
import com.bustech.erp.common.enums.TransactionType;
import com.bustech.erp.common.exception.BusinessException;
import com.bustech.erp.common.exception.ResourceNotFoundException;
import com.bustech.erp.company.service.CompanyService;
import com.bustech.erp.financial.dto.CreateTransactionRequest;
import com.bustech.erp.financial.dto.PayTransactionRequest;
import com.bustech.erp.financial.dto.TransactionResponse;
import com.bustech.erp.financial.entity.FinancialEvent;
import com.bustech.erp.financial.entity.FinancialTransaction;
import com.bustech.erp.financial.repository.FinancialEventRepository;
import com.bustech.erp.financial.repository.FinancialTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialTransactionService {

    private final FinancialTransactionRepository transactionRepository;
    private final FinancialEventRepository eventRepository;
    private final CompanyService companyService;

    public Page<TransactionResponse> findByCompany(Long companyId, Pageable pageable) {
        return transactionRepository.findByCompanyId(companyId, pageable)
            .map(this::toResponse);
    }

    public Page<TransactionResponse> findByCompanyAndType(Long companyId, TransactionType type, Pageable pageable) {
        return transactionRepository.findByCompanyIdAndType(companyId, type, pageable)
            .map(this::toResponse);
    }

    public TransactionResponse findByIdAndCompany(Long id, Long companyId) {
        FinancialTransaction tx = transactionRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Lançamento financeiro", id));
        return toResponse(tx);
    }

    @Transactional
    @SuppressWarnings("null")
    public TransactionResponse create(Long companyId, CreateTransactionRequest request) {
        var company = companyService.findById(companyId);

        FinancialTransaction tx = FinancialTransaction.builder()
            .company(company)
            .type(request.type())
            .description(request.description())
            .amount(request.amount())
            .dueDate(request.dueDate())
            .category(request.category())
            .costCenter(request.costCenter())
            .notes(request.notes())
            .build();

        var saved = transactionRepository.save(tx);

        // Mirror to financial_events so that dashboard analytics reflect this transaction.
        // Only INCOME and EXPENSE types have a dashboard direction; TRANSFER is skipped.
        if (saved.getType() == TransactionType.INCOME || saved.getType() == TransactionType.EXPENSE) {
            FinancialDirection direction = saved.getType() == TransactionType.INCOME
                ? FinancialDirection.INCOME : FinancialDirection.EXPENSE;
            FinancialEvent event = FinancialEvent.builder()
                .company(company)
                .externalId("ft-" + saved.getId())
                .direction(direction)
                .amount(saved.getAmount())
                .description(saved.getDescription())
                .issueDate(LocalDate.now())
                .dueDate(saved.getDueDate())
                .paidDate(saved.getPaymentDate())
                .status(saved.getStatus())
                .build();
            eventRepository.save(event);
        }

        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse pay(Long id, Long companyId, PayTransactionRequest request) {
        FinancialTransaction tx = transactionRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Lançamento financeiro", id));

        if (tx.getStatus() == TransactionStatus.CANCELLED) {
            throw new BusinessException("Não é possível pagar um lançamento cancelado.");
        }
        if (tx.getStatus() == TransactionStatus.PAID) {
            throw new BusinessException("Lançamento já foi quitado.");
        }

        tx.setPaidAmount(request.paidAmount());
        tx.setPaymentDate(request.paymentDate());

        if (request.paidAmount().compareTo(tx.getAmount()) >= 0) {
            tx.setStatus(TransactionStatus.PAID);
        } else {
            tx.setStatus(TransactionStatus.PARTIALLY_PAID);
        }

        FinancialTransaction saved = transactionRepository.save(tx);

        // Sync paid status to the mirrored financial event.
        eventRepository.findByCompanyIdAndExternalId(companyId, "ft-" + saved.getId())
            .ifPresent(event -> {
                event.setStatus(saved.getStatus());
                event.setPaidDate(saved.getPaymentDate());
                eventRepository.save(event);
            });

        return toResponse(saved);
    }

    @Transactional
    public TransactionResponse cancel(Long id, Long companyId) {
        FinancialTransaction tx = transactionRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Lançamento financeiro", id));

        if (tx.getStatus() == TransactionStatus.PAID) {
            throw new BusinessException("Não é possível cancelar um lançamento já pago.");
        }

        tx.setStatus(TransactionStatus.CANCELLED);
        FinancialTransaction saved = transactionRepository.save(tx);

        // Sync cancelled status to the mirrored financial event.
        eventRepository.findByCompanyIdAndExternalId(companyId, "ft-" + saved.getId())
            .ifPresent(event -> {
                event.setStatus(TransactionStatus.CANCELLED);
                eventRepository.save(event);
            });

        return toResponse(saved);
    }

    public void updateOverdueStatuses(Long companyId) {
        transactionRepository.findByCompanyIdAndStatus(companyId, TransactionStatus.PENDING, Pageable.unpaged())
            .forEach(tx -> {
                if (tx.getDueDate().isBefore(LocalDate.now())) {
                    tx.setStatus(TransactionStatus.OVERDUE);
                    transactionRepository.save(tx);
                }
            });
    }

    private TransactionResponse toResponse(FinancialTransaction tx) {
        return new TransactionResponse(
            tx.getId(),
            tx.getCompany().getId(),
            tx.getCompany().getName(),
            tx.getType(),
            tx.getStatus(),
            tx.getDescription(),
            tx.getAmount(),
            tx.getPaidAmount(),
            tx.getDueDate(),
            tx.getPaymentDate(),
            tx.getCategory(),
            tx.getCostCenter(),
            tx.getNotes(),
            tx.getExternalSource(),
            tx.getCreatedAt()
        );
    }
}
