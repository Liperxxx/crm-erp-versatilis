package com.bustech.erp.financial.controller;

import com.bustech.erp.common.enums.FinancialDirection;
import com.bustech.erp.common.exception.ResourceNotFoundException;
import com.bustech.erp.common.response.ApiResponse;
import com.bustech.erp.common.util.SecurityUtils;
import com.bustech.erp.financial.dto.FinancialCategoryResponse;
import com.bustech.erp.financial.entity.FinancialCategory;
import com.bustech.erp.financial.repository.FinancialCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only REST API for financial categories scoped to the authenticated user's company.
 *
 * <p>Categories are populated automatically by the Conta Azul sync
 * (POST /api/integrations/conta-azul/sync/{companyId}).
 *
 * <p>Endpoints:
 * <pre>
 *   GET /api/v1/financial/categories                 — list all active categories
 *   GET /api/v1/financial/categories?activeOnly=false — include inactive (deactivated) categories
 *   GET /api/v1/financial/categories?type=INCOME      — filter by direction
 *   GET /api/v1/financial/categories/{id}             — single category
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/financial/categories")
@RequiredArgsConstructor
public class FinancialCategoryController {

    private final FinancialCategoryRepository categoryRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<FinancialCategoryResponse>>> findAll(
            @RequestParam(required = false) FinancialDirection type,
            @RequestParam(defaultValue = "true") boolean activeOnly) {

        Long companyId = SecurityUtils.getCurrentCompanyId();

        List<FinancialCategory> categories;
        if (type != null && activeOnly) {
            categories = categoryRepository.findByCompanyIdAndTypeAndActive(companyId, type, true);
        } else if (type != null) {
            categories = categoryRepository.findByCompanyIdAndType(companyId, type);
        } else if (activeOnly) {
            categories = categoryRepository.findByCompanyIdAndActive(companyId, true);
        } else {
            categories = categoryRepository.findByCompanyId(companyId);
        }

        return ResponseEntity.ok(ApiResponse.ok(
            categories.stream().map(FinancialCategoryController::toResponse).toList()
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<FinancialCategoryResponse>> findById(@PathVariable Long id) {
        Long companyId = SecurityUtils.getCurrentCompanyId();
        FinancialCategory category = categoryRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Categoria financeira", id));
        return ResponseEntity.ok(ApiResponse.ok(toResponse(category)));
    }

    private static FinancialCategoryResponse toResponse(FinancialCategory c) {
        return new FinancialCategoryResponse(
            c.getId(),
            c.getCompany().getId(),   // safe: Hibernate proxy getId() never triggers lazy load
            c.getExternalId(),
            c.getName(),
            c.getType(),
            c.isActive(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}
