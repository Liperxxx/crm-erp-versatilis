package com.bustech.erp.financial.repository;

import com.bustech.erp.financial.entity.FinancialEventAllocationCostCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface FinancialEventAllocationCostCenterRepository
        extends JpaRepository<FinancialEventAllocationCostCenter, Long> {

    List<FinancialEventAllocationCostCenter> findByAllocationId(Long allocationId);

    // ─── Analíticas por centro de custo ──────────────────────────────────────

    /**
     * Sum of allocated amounts grouped by cost center name,
     * filtered by company, paid date range, and event direction.
     * Used by the cost-center breakdown dashboard.
     */
    @Query("""
        SELECT COALESCE(cc.costCenterName, 'Sem Centro de Custo'),
               COALESCE(SUM(cc.allocatedAmount), 0)
        FROM FinancialEventAllocationCostCenter cc
        JOIN cc.allocation a
        JOIN a.event e
        WHERE cc.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        GROUP BY cc.costCenterName
        ORDER BY SUM(cc.allocatedAmount) DESC
        """)
    List<Object[]> sumByCostCenterNameAndPeriod(
        @Param("companyId") Long companyId,
        @Param("direction") com.bustech.erp.common.enums.FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /**
     * Same but grouped by the local cost_center_id FK (more precise).
     * Returns (costCenterId, costCenterName, sum).
     */
    @Query("""
        SELECT cc.costCenter.id, cc.costCenterName,
               COALESCE(SUM(cc.allocatedAmount), 0)
        FROM FinancialEventAllocationCostCenter cc
        JOIN cc.allocation a
        JOIN a.event e
        WHERE cc.company.id = :companyId
          AND cc.costCenter IS NOT NULL
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        GROUP BY cc.costCenter.id, cc.costCenterName
        ORDER BY SUM(cc.allocatedAmount) DESC
        """)
    List<Object[]> sumByCostCenterIdAndPeriod(
        @Param("companyId") Long companyId,
        @Param("direction") com.bustech.erp.common.enums.FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /**
     * Monthly time series of allocated amounts per cost center name.
     * Returns (year, month, costCenterName, sum).
     */
    @Query("""
        SELECT YEAR(e.paidDate), MONTH(e.paidDate),
               COALESCE(cc.costCenterName, 'Sem Centro de Custo'),
               COALESCE(SUM(cc.allocatedAmount), 0)
        FROM FinancialEventAllocationCostCenter cc
        JOIN cc.allocation a
        JOIN a.event e
        WHERE cc.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        GROUP BY YEAR(e.paidDate), MONTH(e.paidDate), cc.costCenterName
        ORDER BY YEAR(e.paidDate), MONTH(e.paidDate), SUM(cc.allocatedAmount) DESC
        """)
    List<Object[]> monthlySumByCostCenterName(
        @Param("companyId") Long companyId,
        @Param("direction") com.bustech.erp.common.enums.FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /**
     * Cost-center sums from rateio allocations, with an optional category filter.
     *
     * <p>When {@code categoryId} is non-null, only cost-center sub-allocations belonging
     * to an allocation whose resolved local category matches are included.
     * Pass {@code null} to skip this filter.
     */
    @Query("""
        SELECT COALESCE(cc.costCenterName, 'Sem Centro de Custo'),
               COALESCE(SUM(cc.allocatedAmount), 0)
        FROM FinancialEventAllocationCostCenter cc
        JOIN cc.allocation a
        JOIN a.event e
        WHERE cc.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
          AND (:categoryId IS NULL OR a.category.id = :categoryId)
        GROUP BY cc.costCenterName
        ORDER BY SUM(cc.allocatedAmount) DESC
        """)
    List<Object[]> sumByCostCenterNameAndPeriodFiltered(
        @Param("companyId") Long companyId,
        @Param("direction") com.bustech.erp.common.enums.FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("categoryId") Long categoryId
    );

    /**
     * Total allocated amount by company, direction, period — across all cost centers.
     */
    @Query("""
        SELECT COALESCE(SUM(cc.allocatedAmount), 0)
        FROM FinancialEventAllocationCostCenter cc
        JOIN cc.allocation a
        JOIN a.event e
        WHERE cc.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        """)
    BigDecimal sumAllocatedByCompanyAndDirectionAndPeriod(
        @Param("companyId") Long companyId,
        @Param("direction") com.bustech.erp.common.enums.FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
}
