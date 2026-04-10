package com.bustech.erp.financial.repository;

import com.bustech.erp.financial.entity.FinancialEventAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialEventAllocationRepository extends JpaRepository<FinancialEventAllocation, Long> {

    List<FinancialEventAllocation> findByEventId(Long eventId);

    Optional<FinancialEventAllocation> findByEventIdAndExternalCategoryId(
        Long eventId, String externalCategoryId);

    /** Delete all allocations for an event before re-syncing them. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM FinancialEventAllocation a WHERE a.event.id = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);

    // ─── Analíticas por categoria ─────────────────────────────────────────────

    /**
     * Sum of allocated amounts grouped by category (local FK resolved),
     * filtered by company, paid date range, and event direction.
     * Used by the category breakdown dashboard.
     */
    @Query("""
        SELECT COALESCE(a.categoryName, 'Sem Categoria'),
               COALESCE(SUM(a.allocatedAmount), 0)
        FROM FinancialEventAllocation a
        JOIN a.event e
        WHERE a.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        GROUP BY a.categoryName
        ORDER BY SUM(a.allocatedAmount) DESC
        """)
    List<Object[]> sumByCategoryNameAndPeriod(
        @Param("companyId") Long companyId,
        @Param("direction") com.bustech.erp.common.enums.FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /**
     * Same as above but grouped by the local category_id FK (more precise —
     * excludes events where category was not yet resolved to a local entity).
     */
    @Query("""
        SELECT a.category.id, a.categoryName, COALESCE(SUM(a.allocatedAmount), 0)
        FROM FinancialEventAllocation a
        JOIN a.event e
        WHERE a.company.id = :companyId
          AND a.category IS NOT NULL
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        GROUP BY a.category.id, a.categoryName
        ORDER BY SUM(a.allocatedAmount) DESC
        """)
    List<Object[]> sumByCategoryIdAndPeriod(
        @Param("companyId") Long companyId,
        @Param("direction") com.bustech.erp.common.enums.FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /**
     * Monthly time series of allocated amounts per category name.
     * Returns (year, month, categoryName, sum).
     */
    @Query("""
        SELECT YEAR(e.paidDate), MONTH(e.paidDate),
               COALESCE(a.categoryName, 'Sem Categoria'),
               COALESCE(SUM(a.allocatedAmount), 0)
        FROM FinancialEventAllocation a
        JOIN a.event e
        WHERE a.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        GROUP BY YEAR(e.paidDate), MONTH(e.paidDate), a.categoryName
        ORDER BY YEAR(e.paidDate), MONTH(e.paidDate), SUM(a.allocatedAmount) DESC
        """)
    List<Object[]> monthlySumByCategoryName(
        @Param("companyId") Long companyId,
        @Param("direction") com.bustech.erp.common.enums.FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /**
     * Category sums from rateio allocations, with an optional cost-center filter.
     *
     * <p>When {@code costCenterId} is non-null, only allocations belonging to events
     * that have at least one cost-center sub-allocation for the given cost center are
     * included. Pass {@code null} to skip this filter.
     */
    @Query("""
        SELECT COALESCE(a.categoryName, 'Sem Categoria'),
               COALESCE(SUM(a.allocatedAmount), 0)
        FROM FinancialEventAllocation a
        JOIN a.event e
        WHERE a.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
          AND (:costCenterId IS NULL OR EXISTS (
              SELECT 1 FROM FinancialEventAllocationCostCenter cc
              WHERE cc.allocation.id = a.id AND cc.costCenter.id = :costCenterId
          ))
        GROUP BY a.categoryName
        ORDER BY SUM(a.allocatedAmount) DESC
        """)
    List<Object[]> sumByCategoryNameAndPeriodFiltered(
        @Param("companyId") Long companyId,
        @Param("direction") com.bustech.erp.common.enums.FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("costCenterId") Long costCenterId
    );

    /**
     * Total allocated amount by company and period (all directions).
     * Useful for dashboard summary validation against event totals.
     */
    @Query("""
        SELECT COALESCE(SUM(a.allocatedAmount), 0)
        FROM FinancialEventAllocation a
        JOIN a.event e
        WHERE a.company.id = :companyId
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
