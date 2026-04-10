package com.bustech.erp.financial.repository;

import com.bustech.erp.common.enums.FinancialDirection;
import com.bustech.erp.common.enums.TransactionStatus;
import com.bustech.erp.financial.entity.FinancialEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialEventRepository extends JpaRepository<FinancialEvent, Long> {

    Page<FinancialEvent> findByCompanyId(Long companyId, Pageable pageable);

    Page<FinancialEvent> findByCompanyIdAndDirection(Long companyId, FinancialDirection direction, Pageable pageable);

    Page<FinancialEvent> findByCompanyIdAndStatus(Long companyId, TransactionStatus status, Pageable pageable);

    Page<FinancialEvent> findByCompanyIdAndDirectionAndStatus(
        Long companyId, FinancialDirection direction, TransactionStatus status, Pageable pageable);

    Optional<FinancialEvent> findByIdAndCompanyId(Long id, Long companyId);

    Optional<FinancialEvent> findByCompanyIdAndExternalId(Long companyId, String externalId);

    boolean existsByCompanyIdAndExternalId(Long companyId, String externalId);

    /**
     * Returns all events for the given company that were synced from Conta Azul
     * (i.e. have a non-null externalId). Used by parcel-detail sync to drive the
     * per-event detail fetch loop.
     */
    List<FinancialEvent> findByCompanyIdAndExternalIdIsNotNull(Long companyId);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        WHERE e.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        """)
    BigDecimal sumPaidByCompanyAndDirectionAndPeriod(
        @Param("companyId") Long companyId,
        @Param("direction") FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        WHERE e.company.id = :companyId
          AND e.status = 'PENDING'
          AND e.dueDate < :today
        """)
    BigDecimal sumOverdueByCompanyId(
        @Param("companyId") Long companyId,
        @Param("today") LocalDate today
    );

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        WHERE e.company.id = :companyId
          AND e.direction = :direction
          AND e.status NOT IN ('CANCELLED')
          AND e.dueDate BETWEEN :from AND :to
        """)
    BigDecimal sumProjectedByCompanyAndDirectionAndPeriod(
        @Param("companyId") Long companyId,
        @Param("direction") FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    // Dashboard analytics queries

    @Query("""
        SELECT YEAR(e.paidDate), MONTH(e.paidDate), COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        WHERE e.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        GROUP BY YEAR(e.paidDate), MONTH(e.paidDate)
        ORDER BY YEAR(e.paidDate), MONTH(e.paidDate)
        """)
    List<Object[]> findMonthlyPaidSums(
        @Param("companyId") Long companyId,
        @Param("direction") FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    @Query("""
        SELECT COALESCE(c.name, 'Sem Categoria'), COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        LEFT JOIN e.category c
        WHERE e.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        GROUP BY c.name
        ORDER BY SUM(e.amount) DESC
        """)
    List<Object[]> findCategoryPaidSums(
        @Param("companyId") Long companyId,
        @Param("direction") FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /**
     * Category-level paid sums for events that have NO rateio allocations.
     *
     * <p>Used alongside {@code FinancialEventAllocationRepository.sumByCategoryNameAndPeriod}
     * to build a hybrid view: events with rateio contribute via the allocation table;
     * events without rateio contribute here. The two results are merged in the service
     * layer to produce a complete, de-duplicated category breakdown.
     */
    @Query("""
        SELECT COALESCE(c.name, 'Sem Categoria'), COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        LEFT JOIN e.category c
        WHERE e.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
          AND NOT EXISTS (
              SELECT 1 FROM FinancialEventAllocation a WHERE a.event.id = e.id
          )
        GROUP BY c.name
        ORDER BY SUM(e.amount) DESC
        """)
    List<Object[]> findCategoryPaidSumsForEventsWithoutAllocations(
        @Param("companyId") Long companyId,
        @Param("direction") FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    @Query("""
        SELECT COALESCE(cc.name, 'Sem Centro de Custo'), COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        LEFT JOIN e.costCenter cc
        WHERE e.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        GROUP BY cc.name
        ORDER BY SUM(e.amount) DESC
        """)
    List<Object[]> findCostCenterPaidSums(
        @Param("companyId") Long companyId,
        @Param("direction") FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    /**
     * Cost-center-level paid sums for events that have NO rateio allocations.
     *
     * <p>Companion to {@link #findCostCenterPaidSums}: events with rateio allocations
     * are excluded here because their cost-center split is stored accurately in
     * {@code financial_event_allocation_cost_centers}. The two result sets are merged
     * in the service layer to produce a complete, de-duplicated cost-center breakdown.
     */
    @Query("""
        SELECT COALESCE(cc.name, 'Sem Centro de Custo'), COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        LEFT JOIN e.costCenter cc
        WHERE e.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
          AND NOT EXISTS (
              SELECT 1 FROM FinancialEventAllocation a WHERE a.event.id = e.id
          )
        GROUP BY cc.name
        ORDER BY SUM(e.amount) DESC
        """)
    List<Object[]> findCostCenterPaidSumsForEventsWithoutAllocations(
        @Param("companyId") Long companyId,
        @Param("direction") FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    @Query("""
        SELECT YEAR(e.paidDate), MONTH(e.paidDate), e.direction, COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        WHERE e.company.id = :companyId
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        GROUP BY YEAR(e.paidDate), MONTH(e.paidDate), e.direction
        ORDER BY YEAR(e.paidDate), MONTH(e.paidDate)
        """)
    List<Object[]> findMonthlyCashFlowRaw(
        @Param("companyId") Long companyId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    @Query("""
        SELECT COUNT(e)
        FROM FinancialEvent e
        WHERE e.company.id = :companyId
          AND e.status = 'PENDING'
          AND e.dueDate >= :today
        """)
    long countPendingByCompanyId(
        @Param("companyId") Long companyId,
        @Param("today") LocalDate today
    );

    // ─── Filtered analytics queries (optional categoryId / costCenterId) ─────

    /**
     * Sum of paid amounts with optional category and cost-center filters.
     *
     * <p>Pass {@code null} for either filter to omit it.
     * When both are null this is equivalent to {@link #sumPaidByCompanyAndDirectionAndPeriod}.
     */
    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        WHERE e.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
          AND (:categoryId IS NULL OR e.category.id = :categoryId)
          AND (:costCenterId IS NULL OR e.costCenter.id = :costCenterId)
        """)
    BigDecimal sumPaidWithFilters(
        @Param("companyId") Long companyId,
        @Param("direction") FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("categoryId") Long categoryId,
        @Param("costCenterId") Long costCenterId
    );

    /**
     * Monthly paid sums with optional category and cost-center filters.
     *
     * <p>Pass {@code null} for either filter to omit it.
     */
    @Query("""
        SELECT YEAR(e.paidDate), MONTH(e.paidDate), COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        WHERE e.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
          AND (:categoryId IS NULL OR e.category.id = :categoryId)
          AND (:costCenterId IS NULL OR e.costCenter.id = :costCenterId)
        GROUP BY YEAR(e.paidDate), MONTH(e.paidDate)
        ORDER BY YEAR(e.paidDate), MONTH(e.paidDate)
        """)
    List<Object[]> findMonthlyPaidSumsWithFilters(
        @Param("companyId") Long companyId,
        @Param("direction") FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("categoryId") Long categoryId,
        @Param("costCenterId") Long costCenterId
    );

    /**
     * Monthly cash-flow raw data with optional category and cost-center filters.
     *
     * <p>Pass {@code null} for either filter to omit it.
     */
    @Query("""
        SELECT YEAR(e.paidDate), MONTH(e.paidDate), e.direction, COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        WHERE e.company.id = :companyId
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
          AND (:categoryId IS NULL OR e.category.id = :categoryId)
          AND (:costCenterId IS NULL OR e.costCenter.id = :costCenterId)
        GROUP BY YEAR(e.paidDate), MONTH(e.paidDate), e.direction
        ORDER BY YEAR(e.paidDate), MONTH(e.paidDate)
        """)
    List<Object[]> findMonthlyCashFlowRawWithFilters(
        @Param("companyId") Long companyId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("categoryId") Long categoryId,
        @Param("costCenterId") Long costCenterId
    );

    /**
     * Category-level paid sums for events WITHOUT rateio allocations,
     * filtered by an optional cost-center id.
     *
     * <p>Companion to
     * {@link #findCategoryPaidSumsForEventsWithoutAllocations} — used when
     * the caller also wants to scope the category breakdown to a specific cost center.
     * Pass {@code null} to skip the cost-center filter.
     */
    @Query("""
        SELECT COALESCE(c.name, 'Sem Categoria'), COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        LEFT JOIN e.category c
        WHERE e.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
          AND NOT EXISTS (
              SELECT 1 FROM FinancialEventAllocation a WHERE a.event.id = e.id
          )
          AND (:costCenterId IS NULL OR e.costCenter.id = :costCenterId)
        GROUP BY c.name
        ORDER BY SUM(e.amount) DESC
        """)
    List<Object[]> findCategoryPaidSumsForEventsWithoutAllocationsFiltered(
        @Param("companyId") Long companyId,
        @Param("direction") FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("costCenterId") Long costCenterId
    );

    /**
     * Cost-center-level paid sums for events WITHOUT rateio allocations,
     * filtered by an optional category id.
     *
     * <p>Companion to
     * {@link #findCostCenterPaidSumsForEventsWithoutAllocations} — used when
     * the caller also wants to scope the cost-center breakdown to a specific category.
     * Pass {@code null} to skip the category filter.
     */
    @Query("""
        SELECT COALESCE(cc.name, 'Sem Centro de Custo'), COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        LEFT JOIN e.costCenter cc
        WHERE e.company.id = :companyId
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
          AND NOT EXISTS (
              SELECT 1 FROM FinancialEventAllocation a WHERE a.event.id = e.id
          )
          AND (:categoryId IS NULL OR e.category.id = :categoryId)
        GROUP BY cc.name
        ORDER BY SUM(e.amount) DESC
        """)
    List<Object[]> findCostCenterPaidSumsForEventsWithoutAllocationsFiltered(
        @Param("companyId") Long companyId,
        @Param("direction") FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        @Param("categoryId") Long categoryId
    );

    // Consolidated (multi-company) analytics queries

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        WHERE e.company.id IN :companyIds
          AND e.direction = :direction
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        """)
    BigDecimal sumPaidByCompanyIdsAndDirectionAndPeriod(
        @Param("companyIds") List<Long> companyIds,
        @Param("direction") FinancialDirection direction,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    @Query("""
        SELECT e.company.id, e.company.name, COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        WHERE e.company.id IN :companyIds
          AND e.direction = 'INCOME'
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        GROUP BY e.company.id, e.company.name
        ORDER BY SUM(e.amount) DESC
        """)
    List<Object[]> findRevenueSumPerCompanyIds(
        @Param("companyIds") List<Long> companyIds,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );

    @Query("""
        SELECT e.company.id, e.company.name,
               YEAR(e.paidDate), MONTH(e.paidDate),
               e.direction, COALESCE(SUM(e.amount), 0)
        FROM FinancialEvent e
        WHERE e.company.id IN :companyIds
          AND e.status = 'PAID'
          AND e.paidDate BETWEEN :from AND :to
        GROUP BY e.company.id, e.company.name,
                 YEAR(e.paidDate), MONTH(e.paidDate), e.direction
        ORDER BY e.company.id, YEAR(e.paidDate), MONTH(e.paidDate)
        """)
    List<Object[]> findMonthlyPerCompanyCashFlow(
        @Param("companyIds") List<Long> companyIds,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to
    );
}
