package com.bustech.erp.financial.controller;

import com.bustech.erp.common.response.ApiResponse;
import com.bustech.erp.common.response.PageResponse;
import com.bustech.erp.common.util.SecurityUtils;
import com.bustech.erp.financial.dto.CreateTransactionRequest;
import com.bustech.erp.financial.dto.PayTransactionRequest;
import com.bustech.erp.financial.dto.TransactionResponse;
import com.bustech.erp.financial.service.FinancialTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/financial/transactions")
@RequiredArgsConstructor
public class FinancialTransactionController {

    private final FinancialTransactionService transactionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> findAll(
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable) {
        Long companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(ApiResponse.ok(
            PageResponse.from(transactionService.findByCompany(companyId, pageable))
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<TransactionResponse>> findById(@PathVariable Long id) {
        Long companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(ApiResponse.ok(
            transactionService.findByIdAndCompany(id, companyId)
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @Valid @RequestBody CreateTransactionRequest request) {
        Long companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(transactionService.create(companyId, request)));
    }

    @PatchMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TransactionResponse>> pay(
            @PathVariable Long id,
            @Valid @RequestBody PayTransactionRequest request) {
        Long companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(ApiResponse.ok(
            transactionService.pay(id, companyId, request), "Pagamento registrado com sucesso."
        ));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<TransactionResponse>> cancel(@PathVariable Long id) {
        Long companyId = SecurityUtils.getCurrentCompanyId();
        return ResponseEntity.ok(ApiResponse.ok(
            transactionService.cancel(id, companyId), "Lançamento cancelado."
        ));
    }
}
