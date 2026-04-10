package com.bustech.erp.financial.controller;

import com.bustech.erp.common.exception.ResourceNotFoundException;
import com.bustech.erp.common.response.ApiResponse;
import com.bustech.erp.common.util.SecurityUtils;
import com.bustech.erp.financial.dto.CostCenterResponse;
import com.bustech.erp.financial.entity.CostCenter;
import com.bustech.erp.financial.repository.CostCenterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Read-only REST API for cost centers scoped to the authenticated user's company.
 *
 * <p>Cost centers are populated automatically by the Conta Azul sync
 * (POST /api/integrations/conta-azul/sync/{companyId}).
 *
 * <p>Endpoints:
 * <pre>
 *   GET /api/v1/financial/cost-centers                   — list all active cost centers
 *   GET /api/v1/financial/cost-centers?activeOnly=false  — include inactive (deactivated) cost centers
 *   GET /api/v1/financial/cost-centers/{id}              — single cost center
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/financial/cost-centers")
@RequiredArgsConstructor
public class CostCenterController {

    private final CostCenterRepository costCenterRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<List<CostCenterResponse>>> findAll(
            @RequestParam(defaultValue = "true") boolean activeOnly) {

        Long companyId = SecurityUtils.getCurrentCompanyId();

        List<CostCenter> costCenters = activeOnly
            ? costCenterRepository.findByCompanyIdAndActive(companyId, true)
            : costCenterRepository.findByCompanyId(companyId);

        return ResponseEntity.ok(ApiResponse.ok(
            costCenters.stream().map(CostCenterController::toResponse).toList()
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER')")
    public ResponseEntity<ApiResponse<CostCenterResponse>> findById(@PathVariable Long id) {
        Long companyId = SecurityUtils.getCurrentCompanyId();
        CostCenter costCenter = costCenterRepository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Centro de custo", id));
        return ResponseEntity.ok(ApiResponse.ok(toResponse(costCenter)));
    }

    private static CostCenterResponse toResponse(CostCenter cc) {
        return new CostCenterResponse(
            cc.getId(),
            cc.getCompany().getId(),   // safe: Hibernate proxy getId() never triggers lazy load
            cc.getExternalId(),
            cc.getName(),
            cc.isActive(),
            cc.getCreatedAt(),
            cc.getUpdatedAt()
        );
    }
}
