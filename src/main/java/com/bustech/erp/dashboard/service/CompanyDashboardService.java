package com.bustech.erp.dashboard.service;

import com.bustech.erp.common.enums.FinancialDirection;
import com.bustech.erp.common.enums.TransactionStatus;
import com.bustech.erp.company.entity.Company;
import com.bustech.erp.company.repository.CompanyRepository;
import com.bustech.erp.common.exception.ResourceNotFoundException;
import com.bustech.erp.dashboard.dto.*;
import com.bustech.erp.financial.repository.FinancialEventAllocationCostCenterRepository;
import com.bustech.erp.financial.repository.FinancialEventAllocationRepository;
import com.bustech.erp.financial.repository.FinancialEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyDashboardService {

    private final FinancialEventRepository eventRepository;
    private final FinancialEventAllocationRepository allocationRepository;
    private final FinancialEventAllocationCostCenterRepository costCenterAllocationRepository;
    private final CompanyRepository companyRepository;

    // ─── Summary ─────────────────────────────────────────────────────────────

    public DashboardSummaryResponse getSummary(Long companyId, LocalDate start, LocalDate end) {
        Company company = requireCompany(companyId);
        LocalDate today = LocalDate.now();

        BigDecimal revenue  = eventRepository.sumPaidByCompanyAndDirectionAndPeriod(companyId, FinancialDirection.INCOME, start, end);
        BigDecimal expense  = eventRepository.sumPaidByCompanyAndDirectionAndPeriod(companyId, FinancialDirection.EXPENSE, start, end);
        BigDecimal profit   = revenue.subtract(expense);
        BigDecimal margin   = revenue.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : profit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal overdue  = eventRepository.sumOverdueByCompanyId(companyId, today);
        long pendingCount   = eventRepository.countPendingByCompanyId(companyId, today);
        long overdueCount   = eventRepository.findByCompanyIdAndStatus(companyId, TransactionStatus.OVERDUE,
            org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        return new DashboardSummaryResponse(
            companyId, company.getName(), start, end,
            revenue, expense, profit, margin,
            profit,   // cashBalance = net realized in period
            overdue, pendingCount, overdueCount
        );
    }

    /** Filtered variant — pass {@code null} for unused filter. */
    public DashboardSummaryResponse getSummary(Long companyId, LocalDate start, LocalDate end,
                                               Long categoryId, Long costCenterId) {
        if (categoryId == null && costCenterId == null) return getSummary(companyId, start, end);
        Company company = requireCompany(companyId);
        LocalDate today = LocalDate.now();
        BigDecimal revenue = eventRepository.sumPaidWithFilters(companyId, FinancialDirection.INCOME, start, end, categoryId, costCenterId);
        BigDecimal expense = eventRepository.sumPaidWithFilters(companyId, FinancialDirection.EXPENSE, start, end, categoryId, costCenterId);
        BigDecimal profit  = revenue.subtract(expense);
        BigDecimal margin  = revenue.compareTo(BigDecimal.ZERO) == 0
            ? BigDecimal.ZERO
            : profit.divide(revenue, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal overdue = eventRepository.sumOverdueByCompanyId(companyId, today);
        long pendingCount  = eventRepository.countPendingByCompanyId(companyId, today);
        long overdueCount  = eventRepository.findByCompanyIdAndStatus(companyId, TransactionStatus.OVERDUE,
            org.springframework.data.domain.Pageable.unpaged()).getTotalElements();
        return new DashboardSummaryResponse(
            companyId, company.getName(), start, end,
            revenue, expense, profit, margin, profit,
            overdue, pendingCount, overdueCount
        );
    }

    // ─── Monthly Revenue ─────────────────────────────────────────────────────

    public List<MonthlySeriesPoint> getMonthlyRevenue(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        List<Object[]> rows = eventRepository.findMonthlyPaidSums(companyId, FinancialDirection.INCOME, start, end);
        return fillMonthlySeries(rows, start, end);
    }
    /** Filtered variant — pass {@code null} for unused filter. */
    public List<MonthlySeriesPoint> getMonthlyRevenue(Long companyId, LocalDate start, LocalDate end,
                                                      Long categoryId, Long costCenterId) {
        if (categoryId == null && costCenterId == null) return getMonthlyRevenue(companyId, start, end);
        requireCompany(companyId);
        return fillMonthlySeries(
            eventRepository.findMonthlyPaidSumsWithFilters(companyId, FinancialDirection.INCOME, start, end, categoryId, costCenterId),
            start, end);
    }

    // ─── Monthly Expenses ────────────────────────────────────────────────────

    public List<MonthlySeriesPoint> getMonthlyExpenses(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        List<Object[]> rows = eventRepository.findMonthlyPaidSums(companyId, FinancialDirection.EXPENSE, start, end);
        return fillMonthlySeries(rows, start, end);
    }
    /** Filtered variant — pass {@code null} for unused filter. */
    public List<MonthlySeriesPoint> getMonthlyExpenses(Long companyId, LocalDate start, LocalDate end,
                                                       Long categoryId, Long costCenterId) {
        if (categoryId == null && costCenterId == null) return getMonthlyExpenses(companyId, start, end);
        requireCompany(companyId);
        return fillMonthlySeries(
            eventRepository.findMonthlyPaidSumsWithFilters(companyId, FinancialDirection.EXPENSE, start, end, categoryId, costCenterId),
            start, end);
    }

    // ─── Monthly Profit ──────────────────────────────────────────────────────

    public List<MonthlyProfitPoint> getMonthlyProfit(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        List<Object[]> incomeRows  = eventRepository.findMonthlyPaidSums(companyId, FinancialDirection.INCOME, start, end);
        List<Object[]> expenseRows = eventRepository.findMonthlyPaidSums(companyId, FinancialDirection.EXPENSE, start, end);

        Map<String, BigDecimal> incomeMap  = toMonthMap(incomeRows);
        Map<String, BigDecimal> expenseMap = toMonthMap(expenseRows);

        List<MonthlyProfitPoint> result = new ArrayList<>();
        for (YearMonth ym : monthRange(start, end)) {
            String key     = ym.getYear() + "-" + ym.getMonthValue();
            BigDecimal rev = incomeMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal exp = expenseMap.getOrDefault(key, BigDecimal.ZERO);
            result.add(new MonthlyProfitPoint(
                ym.getYear(), ym.getMonthValue(), monthLabel(ym),
                rev, exp, rev.subtract(exp)
            ));
        }
        return result;
    }

    /** Filtered variant — pass {@code null} for unused filter. */
    public List<MonthlyProfitPoint> getMonthlyProfit(Long companyId, LocalDate start, LocalDate end,
                                                     Long categoryId, Long costCenterId) {
        if (categoryId == null && costCenterId == null) return getMonthlyProfit(companyId, start, end);
        requireCompany(companyId);
        Map<String, BigDecimal> incomeMap  = toMonthMap(
            eventRepository.findMonthlyPaidSumsWithFilters(companyId, FinancialDirection.INCOME, start, end, categoryId, costCenterId));
        Map<String, BigDecimal> expenseMap = toMonthMap(
            eventRepository.findMonthlyPaidSumsWithFilters(companyId, FinancialDirection.EXPENSE, start, end, categoryId, costCenterId));
        List<MonthlyProfitPoint> result = new ArrayList<>();
        for (YearMonth ym : monthRange(start, end)) {
            String key = ym.getYear() + "-" + ym.getMonthValue();
            BigDecimal rev = incomeMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal exp = expenseMap.getOrDefault(key, BigDecimal.ZERO);
            result.add(new MonthlyProfitPoint(ym.getYear(), ym.getMonthValue(), monthLabel(ym), rev, exp, rev.subtract(exp)));
        }
        return result;
    }

    // ─── Category Breakdown ──────────────────────────────────────────────────

    public CategoryBreakdownResponse getCategoryBreakdown(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        List<Object[]> incRows = eventRepository.findCategoryPaidSums(companyId, FinancialDirection.INCOME, start, end);
        List<Object[]> expRows = eventRepository.findCategoryPaidSums(companyId, FinancialDirection.EXPENSE, start, end);

        BigDecimal totalInc = sum(incRows);
        BigDecimal totalExp = sum(expRows);

        return new CategoryBreakdownResponse(
            toCategoryItems(incRows, totalInc),
            toCategoryItems(expRows, totalExp),
            totalInc, totalExp
        );
    }

    // ─── Category Analytics (hybrid: rateio + direct FK) ────────────────────

    /**
     * Full per-category view: revenue, expenses, profit, and both percentuals.
     * Categories are sorted by totalRevenue descending.
     * Uses rateio allocations when present; falls back to the event category FK otherwise.
     */
    public CategoryAnalyticsResponse getCategoryAnalyticsSummary(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        Map<String, BigDecimal> revMap = buildHybridCategoryMap(companyId, FinancialDirection.INCOME, start, end);
        Map<String, BigDecimal> expMap = buildHybridCategoryMap(companyId, FinancialDirection.EXPENSE, start, end);
        return buildCategoryAnalyticsResponse(revMap, expMap, start, end,
            Comparator.<CategoryAnalyticsItem, BigDecimal>comparing(CategoryAnalyticsItem::totalRevenue).reversed());
    }

    /** Filtered by cost center — pass {@code null} to omit. */
    public CategoryAnalyticsResponse getCategoryAnalyticsSummary(Long companyId, LocalDate start, LocalDate end,
                                                                  Long costCenterId) {
        if (costCenterId == null) return getCategoryAnalyticsSummary(companyId, start, end);
        requireCompany(companyId);
        Map<String, BigDecimal> revMap = buildHybridCategoryMap(companyId, FinancialDirection.INCOME, start, end, costCenterId);
        Map<String, BigDecimal> expMap = buildHybridCategoryMap(companyId, FinancialDirection.EXPENSE, start, end, costCenterId);
        return buildCategoryAnalyticsResponse(revMap, expMap, start, end,
            Comparator.<CategoryAnalyticsItem, BigDecimal>comparing(CategoryAnalyticsItem::totalRevenue).reversed());
    }

    /** Revenue-only view per category, sorted by totalRevenue descending. */
    public CategoryAnalyticsResponse getCategoryRevenue(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        Map<String, BigDecimal> revMap = buildHybridCategoryMap(companyId, FinancialDirection.INCOME, start, end);
        Map<String, BigDecimal> expMap = Collections.emptyMap();
        return buildCategoryAnalyticsResponse(revMap, expMap, start, end,
            Comparator.<CategoryAnalyticsItem, BigDecimal>comparing(CategoryAnalyticsItem::totalRevenue).reversed());
    }

    /** Filtered by cost center — pass {@code null} to omit. */
    public CategoryAnalyticsResponse getCategoryRevenue(Long companyId, LocalDate start, LocalDate end,
                                                        Long costCenterId) {
        if (costCenterId == null) return getCategoryRevenue(companyId, start, end);
        requireCompany(companyId);
        Map<String, BigDecimal> revMap = buildHybridCategoryMap(companyId, FinancialDirection.INCOME, start, end, costCenterId);
        return buildCategoryAnalyticsResponse(revMap, Collections.emptyMap(), start, end,
            Comparator.<CategoryAnalyticsItem, BigDecimal>comparing(CategoryAnalyticsItem::totalRevenue).reversed());
    }

    /** Expenses-only view per category, sorted by totalExpenses descending. */
    public CategoryAnalyticsResponse getCategoryExpenses(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        Map<String, BigDecimal> revMap = Collections.emptyMap();
        Map<String, BigDecimal> expMap = buildHybridCategoryMap(companyId, FinancialDirection.EXPENSE, start, end);
        return buildCategoryAnalyticsResponse(revMap, expMap, start, end,
            Comparator.<CategoryAnalyticsItem, BigDecimal>comparing(CategoryAnalyticsItem::totalExpenses).reversed());
    }

    /** Filtered by cost center — pass {@code null} to omit. */
    public CategoryAnalyticsResponse getCategoryExpenses(Long companyId, LocalDate start, LocalDate end,
                                                         Long costCenterId) {
        if (costCenterId == null) return getCategoryExpenses(companyId, start, end);
        requireCompany(companyId);
        Map<String, BigDecimal> expMap = buildHybridCategoryMap(companyId, FinancialDirection.EXPENSE, start, end, costCenterId);
        return buildCategoryAnalyticsResponse(Collections.emptyMap(), expMap, start, end,
            Comparator.<CategoryAnalyticsItem, BigDecimal>comparing(CategoryAnalyticsItem::totalExpenses).reversed());
    }

    /** Per-category profit view (revenue - expenses), sorted by totalProfit descending. */
    public CategoryAnalyticsResponse getCategoryProfit(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        Map<String, BigDecimal> revMap = buildHybridCategoryMap(companyId, FinancialDirection.INCOME, start, end);
        Map<String, BigDecimal> expMap = buildHybridCategoryMap(companyId, FinancialDirection.EXPENSE, start, end);
        return buildCategoryAnalyticsResponse(revMap, expMap, start, end,
            Comparator.<CategoryAnalyticsItem, BigDecimal>comparing(CategoryAnalyticsItem::totalProfit).reversed());
    }

    /** Filtered by cost center — pass {@code null} to omit. */
    public CategoryAnalyticsResponse getCategoryProfit(Long companyId, LocalDate start, LocalDate end,
                                                       Long costCenterId) {
        if (costCenterId == null) return getCategoryProfit(companyId, start, end);
        requireCompany(companyId);
        Map<String, BigDecimal> revMap = buildHybridCategoryMap(companyId, FinancialDirection.INCOME, start, end, costCenterId);
        Map<String, BigDecimal> expMap = buildHybridCategoryMap(companyId, FinancialDirection.EXPENSE, start, end, costCenterId);
        return buildCategoryAnalyticsResponse(revMap, expMap, start, end,
            Comparator.<CategoryAnalyticsItem, BigDecimal>comparing(CategoryAnalyticsItem::totalProfit).reversed());
    }

    /**
     * Merges category sums from two sources for a given direction:
     * <ol>
     *   <li>Events that HAVE rateio allocations — amounts come from the allocation table,
     *       preserving the exact rateio split per category.</li>
     *   <li>Events that HAVE NO rateio allocations — amounts come directly from the event
     *       amount via the event's primary category FK.</li>
     * </ol>
     * The two result sets are combined (additive merge) into a single Map keyed by category name.
     */
    private Map<String, BigDecimal> buildHybridCategoryMap(
            Long companyId, FinancialDirection direction, LocalDate start, LocalDate end) {

        Map<String, BigDecimal> map = new LinkedHashMap<>();

        // Source 1: rateio allocations (events WITH rateio)
        List<Object[]> fromAlloc =
            allocationRepository.sumByCategoryNameAndPeriod(companyId, direction, start, end);
        for (Object[] row : fromAlloc) {
            String name = (String) row[0];
            BigDecimal amt = (BigDecimal) row[1];
            map.merge(name, amt, BigDecimal::add);
        }

        // Source 2: direct category FK (events WITHOUT rateio)
        List<Object[]> fromDirect =
            eventRepository.findCategoryPaidSumsForEventsWithoutAllocations(companyId, direction, start, end);
        for (Object[] row : fromDirect) {
            String name = (String) row[0];
            BigDecimal amt = (BigDecimal) row[1];
            map.merge(name, amt, BigDecimal::add);
        }

        return map;
    }

    /**
     * Filtered variant of {@link #buildHybridCategoryMap}: only includes events/allocations
     * where the given cost center is involved. Pass {@code null} to skip filtering.
     */
    private Map<String, BigDecimal> buildHybridCategoryMap(
            Long companyId, FinancialDirection direction, LocalDate start, LocalDate end,
            Long costCenterId) {

        Map<String, BigDecimal> map = new LinkedHashMap<>();

        for (Object[] row : allocationRepository.sumByCategoryNameAndPeriodFiltered(companyId, direction, start, end, costCenterId)) {
            map.merge((String) row[0], (BigDecimal) row[1], BigDecimal::add);
        }
        for (Object[] row : eventRepository.findCategoryPaidSumsForEventsWithoutAllocationsFiltered(companyId, direction, start, end, costCenterId)) {
            map.merge((String) row[0], (BigDecimal) row[1], BigDecimal::add);
        }
        return map;
    }

    /**
     * Builds the final response from revenue and expense category maps.
     * Computes grand totals and per-category percentuals.
     */
    private CategoryAnalyticsResponse buildCategoryAnalyticsResponse(
            Map<String, BigDecimal> revMap,
            Map<String, BigDecimal> expMap,
            LocalDate start,
            LocalDate end,
            Comparator<CategoryAnalyticsItem> sort) {

        // All category names from both maps
        Set<String> allCategories = new LinkedHashSet<>();
        allCategories.addAll(revMap.keySet());
        allCategories.addAll(expMap.keySet());

        BigDecimal grandRev = revMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandExp = expMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandProfit = grandRev.subtract(grandExp);

        List<CategoryAnalyticsItem> items = allCategories.stream()
            .map(cat -> {
                BigDecimal rev = revMap.getOrDefault(cat, BigDecimal.ZERO);
                BigDecimal exp = expMap.getOrDefault(cat, BigDecimal.ZERO);
                BigDecimal profit = rev.subtract(exp);
                BigDecimal pctRev = pct(rev, grandRev);
                BigDecimal pctExp = pct(exp, grandExp);
                return new CategoryAnalyticsItem(cat, rev, exp, profit, pctRev, pctExp);
            })
            .sorted(sort)
            .toList();

        return new CategoryAnalyticsResponse(start, end, grandRev, grandExp, grandProfit, items);
    }

    private BigDecimal pct(BigDecimal part, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return part.divide(total, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }

    // ─── Cost Center Analytics (hybrid: rateio + direct FK) ──────────────────

    /** Full per-cost-center view (revenue + expenses + amounts). Sorted by revenue desc. */
    public CostCenterAnalyticsResponse getCostCenterAnalyticsSummary(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        Map<String, BigDecimal> revMap = buildHybridCostCenterMap(companyId, FinancialDirection.INCOME, start, end);
        Map<String, BigDecimal> expMap = buildHybridCostCenterMap(companyId, FinancialDirection.EXPENSE, start, end);
        return buildCostCenterAnalyticsResponse(revMap, expMap, start, end,
            Comparator.<CostCenterAnalyticsItem, BigDecimal>comparing(CostCenterAnalyticsItem::totalRevenue).reversed());
    }

    /** Filtered by category — pass {@code null} to omit. */
    public CostCenterAnalyticsResponse getCostCenterAnalyticsSummary(Long companyId, LocalDate start, LocalDate end,
                                                                      Long categoryId) {
        if (categoryId == null) return getCostCenterAnalyticsSummary(companyId, start, end);
        requireCompany(companyId);
        Map<String, BigDecimal> revMap = buildHybridCostCenterMap(companyId, FinancialDirection.INCOME, start, end, categoryId);
        Map<String, BigDecimal> expMap = buildHybridCostCenterMap(companyId, FinancialDirection.EXPENSE, start, end, categoryId);
        return buildCostCenterAnalyticsResponse(revMap, expMap, start, end,
            Comparator.<CostCenterAnalyticsItem, BigDecimal>comparing(CostCenterAnalyticsItem::totalRevenue).reversed());
    }

    /** Revenue only per cost-center, sorted by totalRevenue desc. */
    public CostCenterAnalyticsResponse getCostCenterRevenue(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        Map<String, BigDecimal> revMap = buildHybridCostCenterMap(companyId, FinancialDirection.INCOME, start, end);
        return buildCostCenterAnalyticsResponse(revMap, Collections.emptyMap(), start, end,
            Comparator.<CostCenterAnalyticsItem, BigDecimal>comparing(CostCenterAnalyticsItem::totalRevenue).reversed());
    }

    /** Filtered by category — pass {@code null} to omit. */
    public CostCenterAnalyticsResponse getCostCenterRevenue(Long companyId, LocalDate start, LocalDate end,
                                                             Long categoryId) {
        if (categoryId == null) return getCostCenterRevenue(companyId, start, end);
        requireCompany(companyId);
        Map<String, BigDecimal> revMap = buildHybridCostCenterMap(companyId, FinancialDirection.INCOME, start, end, categoryId);
        return buildCostCenterAnalyticsResponse(revMap, Collections.emptyMap(), start, end,
            Comparator.<CostCenterAnalyticsItem, BigDecimal>comparing(CostCenterAnalyticsItem::totalRevenue).reversed());
    }

    /** Expenses only per cost-center, sorted by totalExpenses desc. */
    public CostCenterAnalyticsResponse getCostCenterExpenses(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        Map<String, BigDecimal> expMap = buildHybridCostCenterMap(companyId, FinancialDirection.EXPENSE, start, end);
        return buildCostCenterAnalyticsResponse(Collections.emptyMap(), expMap, start, end,
            Comparator.<CostCenterAnalyticsItem, BigDecimal>comparing(CostCenterAnalyticsItem::totalExpenses).reversed());
    }

    /** Filtered by category — pass {@code null} to omit. */
    public CostCenterAnalyticsResponse getCostCenterExpenses(Long companyId, LocalDate start, LocalDate end,
                                                              Long categoryId) {
        if (categoryId == null) return getCostCenterExpenses(companyId, start, end);
        requireCompany(companyId);
        Map<String, BigDecimal> expMap = buildHybridCostCenterMap(companyId, FinancialDirection.EXPENSE, start, end, categoryId);
        return buildCostCenterAnalyticsResponse(Collections.emptyMap(), expMap, start, end,
            Comparator.<CostCenterAnalyticsItem, BigDecimal>comparing(CostCenterAnalyticsItem::totalExpenses).reversed());
    }

    /**
     * Merges cost-center sums from two sources for a given direction:
     * <ol>
     *   <li>Events WITH rateio → amounts from {@code financial_event_allocation_cost_centers}.</li>
     *   <li>Events WITHOUT rateio → amounts from the event’s primary cost-center FK.</li>
     * </ol>
     */
    private Map<String, BigDecimal> buildHybridCostCenterMap(
            Long companyId, FinancialDirection direction, LocalDate start, LocalDate end) {

        Map<String, BigDecimal> map = new LinkedHashMap<>();

        for (Object[] row : costCenterAllocationRepository.sumByCostCenterNameAndPeriod(companyId, direction, start, end)) {
            map.merge((String) row[0], (BigDecimal) row[1], BigDecimal::add);
        }
        for (Object[] row : eventRepository.findCostCenterPaidSumsForEventsWithoutAllocations(companyId, direction, start, end)) {
            map.merge((String) row[0], (BigDecimal) row[1], BigDecimal::add);
        }
        return map;
    }

    /**
     * Filtered variant of {@link #buildHybridCostCenterMap}: only includes events/allocations
     * where the given category is involved. Pass {@code null} to skip filtering.
     */
    private Map<String, BigDecimal> buildHybridCostCenterMap(
            Long companyId, FinancialDirection direction, LocalDate start, LocalDate end,
            Long categoryId) {

        Map<String, BigDecimal> map = new LinkedHashMap<>();

        for (Object[] row : costCenterAllocationRepository.sumByCostCenterNameAndPeriodFiltered(companyId, direction, start, end, categoryId)) {
            map.merge((String) row[0], (BigDecimal) row[1], BigDecimal::add);
        }
        for (Object[] row : eventRepository.findCostCenterPaidSumsForEventsWithoutAllocationsFiltered(companyId, direction, start, end, categoryId)) {
            map.merge((String) row[0], (BigDecimal) row[1], BigDecimal::add);
        }
        return map;
    }

    private CostCenterAnalyticsResponse buildCostCenterAnalyticsResponse(
            Map<String, BigDecimal> revMap, Map<String, BigDecimal> expMap,
            LocalDate start, LocalDate end, Comparator<CostCenterAnalyticsItem> sort) {

        Set<String> allNames = new LinkedHashSet<>();
        allNames.addAll(revMap.keySet());
        allNames.addAll(expMap.keySet());

        BigDecimal grandRev = revMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grandExp = expMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CostCenterAnalyticsItem> items = allNames.stream()
            .map(name -> {
                BigDecimal rev = revMap.getOrDefault(name, BigDecimal.ZERO);
                BigDecimal exp = expMap.getOrDefault(name, BigDecimal.ZERO);
                return new CostCenterAnalyticsItem(name, rev, exp,
                    pct(rev, grandRev), pct(exp, grandExp));
            })
            .sorted(sort)
            .toList();

        return new CostCenterAnalyticsResponse(start, end, grandRev, grandExp, items);
    }

    // ─── Cost Center Breakdown ───────────────────────────────────────────────

    public CostCenterBreakdownResponse getCostCenterBreakdown(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        List<Object[]> incRows = eventRepository.findCostCenterPaidSums(companyId, FinancialDirection.INCOME, start, end);
        List<Object[]> expRows = eventRepository.findCostCenterPaidSums(companyId, FinancialDirection.EXPENSE, start, end);

        BigDecimal totalInc = sum(incRows);
        BigDecimal totalExp = sum(expRows);

        return new CostCenterBreakdownResponse(
            toCostCenterItems(incRows, totalInc),
            toCostCenterItems(expRows, totalExp),
            totalInc, totalExp
        );
    }

    // ─── Cash Flow ───────────────────────────────────────────────────────────

    public CashFlowResponse getCashFlow(Long companyId, LocalDate start, LocalDate end) {
        requireCompany(companyId);
        List<Object[]> rows = eventRepository.findMonthlyCashFlowRaw(companyId, start, end);        return buildCashFlowResponse(rows, start, end);
    }

    /** Filtered variant — pass {@code null} for unused filter. */
    public CashFlowResponse getCashFlow(Long companyId, LocalDate start, LocalDate end,
                                        Long categoryId, Long costCenterId) {
        if (categoryId == null && costCenterId == null) return getCashFlow(companyId, start, end);
        requireCompany(companyId);
        List<Object[]> rows = eventRepository.findMonthlyCashFlowRawWithFilters(companyId, start, end, categoryId, costCenterId);
        return buildCashFlowResponse(rows, start, end);
    }

    private CashFlowResponse buildCashFlowResponse(List<Object[]> rows, LocalDate start, LocalDate end) {
        // rows: (year, month, direction, amount)
        Map<String, BigDecimal[]> monthData = new LinkedHashMap<>();
        for (Object[] row : rows) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            FinancialDirection dir = (FinancialDirection) row[2];
            BigDecimal amount = (BigDecimal) row[3];
            String key = year + "-" + month;
            monthData.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (dir == FinancialDirection.INCOME) {
                monthData.get(key)[0] = amount;
            } else {
                monthData.get(key)[1] = amount;
            }
        }

        List<CashFlowPoint> points = new ArrayList<>();
        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal totalIncome  = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (YearMonth ym : monthRange(start, end)) {
            String key = ym.getYear() + "-" + ym.getMonthValue();
            BigDecimal[] data = monthData.getOrDefault(key, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal inc = data[0];
            BigDecimal exp = data[1];
            BigDecimal net = inc.subtract(exp);
            cumulative = cumulative.add(net);
            totalIncome  = totalIncome.add(inc);
            totalExpense = totalExpense.add(exp);
            points.add(new CashFlowPoint(ym.getYear(), ym.getMonthValue(), monthLabel(ym), inc, exp, net, cumulative));
        }

        return new CashFlowResponse(points, totalIncome, totalExpense, totalIncome.subtract(totalExpense));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Company requireCompany(Long companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId));
    }

    private List<MonthlySeriesPoint> fillMonthlySeries(List<Object[]> rows, LocalDate start, LocalDate end) {
        Map<String, BigDecimal> map = toMonthMap(rows);
        List<MonthlySeriesPoint> result = new ArrayList<>();
        for (YearMonth ym : monthRange(start, end)) {
            String key = ym.getYear() + "-" + ym.getMonthValue();
            result.add(new MonthlySeriesPoint(
                ym.getYear(), ym.getMonthValue(), monthLabel(ym),
                map.getOrDefault(key, BigDecimal.ZERO)
            ));
        }
        return result;
    }

    private Map<String, BigDecimal> toMonthMap(List<Object[]> rows) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            int year  = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            BigDecimal amount = (BigDecimal) row[2];
            map.put(year + "-" + month, amount);
        }
        return map;
    }

    private List<YearMonth> monthRange(LocalDate start, LocalDate end) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth from = YearMonth.from(start);
        YearMonth to   = YearMonth.from(end);
        YearMonth cur  = from;
        while (!cur.isAfter(to)) {
            months.add(cur);
            cur = cur.plusMonths(1);
        }
        return months;
    }

    private String monthLabel(YearMonth ym) {
        return Month.of(ym.getMonthValue())
            .getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"))
            + "/" + String.valueOf(ym.getYear()).substring(2);
    }

    private BigDecimal sum(List<Object[]> rows) {
        return rows.stream()
            .map(r -> (BigDecimal) r[1])
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CategoryBreakdownItem> toCategoryItems(List<Object[]> rows, BigDecimal total) {
        return rows.stream().map(row -> {
            String name     = (String) row[0];
            BigDecimal amt  = (BigDecimal) row[1];
            BigDecimal pct  = total.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : amt.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            return new CategoryBreakdownItem(name, amt, pct);
        }).toList();
    }

    private List<CostCenterBreakdownItem> toCostCenterItems(List<Object[]> rows, BigDecimal total) {
        return rows.stream().map(row -> {
            String name    = (String) row[0];
            BigDecimal amt = (BigDecimal) row[1];
            BigDecimal pct = total.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : amt.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
            return new CostCenterBreakdownItem(name, amt, pct);
        }).toList();
    }
}