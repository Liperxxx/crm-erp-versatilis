package com.bustech.erp.dashboard.service;

import com.bustech.erp.common.exception.ResourceNotFoundException;
import com.bustech.erp.company.repository.CompanyRepository;
import com.bustech.erp.dashboard.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComparisonDashboardService {

    private final CompanyDashboardService companyDashboardService;
    private final CompanyRepository companyRepository;

    // ─── Summary ─────────────────────────────────────────────────────────────

    public ComparisonSummaryResponse getSummary(Long companyAId, Long companyBId,
                                                LocalDate start, LocalDate end) {
        DashboardSummaryResponse a = companyDashboardService.getSummary(companyAId, start, end);
        DashboardSummaryResponse b = companyDashboardService.getSummary(companyBId, start, end);

        CompanySnapshotDto snapA = toSnapshot(a);
        CompanySnapshotDto snapB = toSnapshot(b);

        return new ComparisonSummaryResponse(
            start, end, snapA, snapB,
            winner(snapA.companyName(), snapB.companyName(), snapA.revenue(),  snapB.revenue()),
            winner(snapA.companyName(), snapB.companyName(), snapA.expense(),  snapB.expense()),
            winner(snapA.companyName(), snapB.companyName(), snapA.profit(),   snapB.profit()),
            winner(snapA.companyName(), snapB.companyName(), snapA.margin(),   snapB.margin()),
            diffPct(snapA.revenue(),  snapB.revenue()),
            diffPct(snapA.expense(),  snapB.expense()),
            diffPct(snapA.profit(),   snapB.profit()),
            diffPct(snapA.margin(),   snapB.margin())
        );
    }

    // ─── Monthly Results (revenue + expense + profit per company per month) ──

    public MonthlyResultsResponse getMonthlyResults(Long companyAId, Long companyBId,
                                                    LocalDate start, LocalDate end) {
        String nameA = requireCompanyName(companyAId);
        String nameB = requireCompanyName(companyBId);

        List<MonthlyProfitPoint> seriesA = companyDashboardService.getMonthlyProfit(companyAId, start, end);
        List<MonthlyProfitPoint> seriesB = companyDashboardService.getMonthlyProfit(companyBId, start, end);

        List<MonthlyResultsPoint> points = new ArrayList<>(seriesA.size());
        for (int i = 0; i < seriesA.size(); i++) {
            MonthlyProfitPoint pa = seriesA.get(i);
            MonthlyProfitPoint pb = seriesB.get(i);
            points.add(new MonthlyResultsPoint(
                pa.year(), pa.month(), pa.monthLabel(),
                pa.revenue(), pa.expense(), pa.profit(),
                pb.revenue(), pb.expense(), pb.profit()
            ));
        }
        return new MonthlyResultsResponse(nameA, nameB, points);
    }

    // ─── Monthly Revenue comparison ──────────────────────────────────────────

    public MonthlyComparisonResponse getMonthlyRevenue(Long companyAId, Long companyBId,
                                                       LocalDate start, LocalDate end) {
        String nameA = requireCompanyName(companyAId);
        String nameB = requireCompanyName(companyBId);
        List<MonthlySeriesPoint> seriesA = companyDashboardService.getMonthlyRevenue(companyAId, start, end);
        List<MonthlySeriesPoint> seriesB = companyDashboardService.getMonthlyRevenue(companyBId, start, end);
        return buildSeriesComparison(nameA, nameB, seriesA, seriesB);
    }

    // ─── Monthly Expenses comparison ─────────────────────────────────────────

    public MonthlyComparisonResponse getMonthlyExpenses(Long companyAId, Long companyBId,
                                                        LocalDate start, LocalDate end) {
        String nameA = requireCompanyName(companyAId);
        String nameB = requireCompanyName(companyBId);
        List<MonthlySeriesPoint> seriesA = companyDashboardService.getMonthlyExpenses(companyAId, start, end);
        List<MonthlySeriesPoint> seriesB = companyDashboardService.getMonthlyExpenses(companyBId, start, end);
        return buildSeriesComparison(nameA, nameB, seriesA, seriesB);
    }

    // ─── Monthly Margins comparison ──────────────────────────────────────────

    public MonthlyComparisonResponse getMonthlyMargins(Long companyAId, Long companyBId,
                                                       LocalDate start, LocalDate end) {
        String nameA = requireCompanyName(companyAId);
        String nameB = requireCompanyName(companyBId);

        List<MonthlyProfitPoint> seriesA = companyDashboardService.getMonthlyProfit(companyAId, start, end);
        List<MonthlyProfitPoint> seriesB = companyDashboardService.getMonthlyProfit(companyBId, start, end);

        List<MonthlyComparisonPoint> points = new ArrayList<>(seriesA.size());
        BigDecimal sumA = BigDecimal.ZERO;
        BigDecimal sumB = BigDecimal.ZERO;

        for (int i = 0; i < seriesA.size(); i++) {
            MonthlyProfitPoint pa = seriesA.get(i);
            MonthlyProfitPoint pb = seriesB.get(i);
            BigDecimal marginA = computeMargin(pa.revenue(), pa.profit());
            BigDecimal marginB = computeMargin(pb.revenue(), pb.profit());
            sumA = sumA.add(marginA);
            sumB = sumB.add(marginB);
            points.add(new MonthlyComparisonPoint(
                pa.year(), pa.month(), pa.monthLabel(),
                marginA, marginB, diffPct(marginA, marginB)
            ));
        }

        // totalA/B for margins = period average margin %
        int n = points.isEmpty() ? 1 : points.size();
        BigDecimal avgA = sumA.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        BigDecimal avgB = sumB.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        return new MonthlyComparisonResponse(nameA, nameB, points, avgA, avgB, diffPct(avgA, avgB));
    }

    // ─── Category Analytics Comparison (hybrid: rateio + direct FK) ─────────

    /**
     * Per-category profit comparison (revenue − expenses) between two companies.
     *
     * <p>Uses the hybrid data source: rateio allocations when present, otherwise
     * the event's primary category FK — identical to the per-company analytics endpoints.
     * Categories that exist for only one company appear with 0 for the other.
     * Items are sorted by combined magnitude (|A| + |B|) descending.
     */
    public CategoryAnalyticsComparisonResponse getCategoryAnalyticsSummary(
            Long companyAId, Long companyBId, LocalDate start, LocalDate end) {

        String nameA = requireCompanyName(companyAId);
        String nameB = requireCompanyName(companyBId);

        // Use profit (revenue - expenses) per category as the "summary" metric
        Map<String, BigDecimal> mapA = toProfitMap(
            companyDashboardService.getCategoryAnalyticsSummary(companyAId, start, end));
        Map<String, BigDecimal> mapB = toProfitMap(
            companyDashboardService.getCategoryAnalyticsSummary(companyBId, start, end));

        return buildCategoryAnalyticsComparison(nameA, nameB, "PROFIT", mapA, mapB, start, end);
    }

    /**
     * Per-category revenue comparison between two companies using rateio-aware hybrid query.
     *
     * <p>Categories present in only one company appear with 0 for the other.
     * Items are sorted by combined revenue (A + B) descending.
     */
    public CategoryAnalyticsComparisonResponse getCategoryAnalyticsRevenue(
            Long companyAId, Long companyBId, LocalDate start, LocalDate end) {

        String nameA = requireCompanyName(companyAId);
        String nameB = requireCompanyName(companyBId);

        Map<String, BigDecimal> mapA = toRevenueMap(
            companyDashboardService.getCategoryRevenue(companyAId, start, end));
        Map<String, BigDecimal> mapB = toRevenueMap(
            companyDashboardService.getCategoryRevenue(companyBId, start, end));

        return buildCategoryAnalyticsComparison(nameA, nameB, "REVENUE", mapA, mapB, start, end);
    }

    /**
     * Per-category expense comparison between two companies using rateio-aware hybrid query.
     *
     * <p>Categories present in only one company appear with 0 for the other.
     * Items are sorted by combined expense (A + B) descending.
     */
    public CategoryAnalyticsComparisonResponse getCategoryAnalyticsExpenses(
            Long companyAId, Long companyBId, LocalDate start, LocalDate end) {

        String nameA = requireCompanyName(companyAId);
        String nameB = requireCompanyName(companyBId);

        Map<String, BigDecimal> mapA = toExpenseMap(
            companyDashboardService.getCategoryExpenses(companyAId, start, end));
        Map<String, BigDecimal> mapB = toExpenseMap(
            companyDashboardService.getCategoryExpenses(companyBId, start, end));

        return buildCategoryAnalyticsComparison(nameA, nameB, "EXPENSE", mapA, mapB, start, end);
    }

    /**
     * Merges two category→amount maps into a {@link CategoryAnalyticsComparisonResponse}.
     *
     * <p>All category names from both companies are included. For categories present in only
     * one company, the other side is 0. Grand totals and per-item difference/% are computed.
     * Items sorted by (|A| + |B|) descending so the most significant categories appear first.
     */
    private CategoryAnalyticsComparisonResponse buildCategoryAnalyticsComparison(
            String nameA, String nameB, String metric,
            Map<String, BigDecimal> mapA, Map<String, BigDecimal> mapB,
            LocalDate start, LocalDate end) {

        // Preserve insertion order: A's categories first, then B-only extras
        Map<String, BigDecimal[]> merged = new LinkedHashMap<>();
        mapA.forEach((k, v) ->
            merged.computeIfAbsent(k, x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[0] = v);
        mapB.forEach((k, v) ->
            merged.computeIfAbsent(k, x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[1] = v);

        BigDecimal totalA = BigDecimal.ZERO;
        BigDecimal totalB = BigDecimal.ZERO;
        List<CategoryAnalyticsComparisonItem> items = new ArrayList<>();

        for (Map.Entry<String, BigDecimal[]> entry : merged.entrySet()) {
            BigDecimal a = entry.getValue()[0];
            BigDecimal b = entry.getValue()[1];
            totalA = totalA.add(a);
            totalB = totalB.add(b);
            items.add(new CategoryAnalyticsComparisonItem(
                entry.getKey(), a, b, a.subtract(b), diffPct(a, b)));
        }

        // Sort by combined magnitude descending — most significant category first
        items.sort((x, y) ->
            y.companyAValue().abs().add(y.companyBValue().abs())
             .compareTo(x.companyAValue().abs().add(x.companyBValue().abs())));

        BigDecimal diff = totalA.subtract(totalB);
        return new CategoryAnalyticsComparisonResponse(
            start, end, nameA, nameB, metric, items,
            totalA, totalB, diff, diffPct(totalA, totalB));
    }

    // ─── Extraction helpers ───────────────────────────────────────────────────

    /** Extracts categoryName → totalRevenue from a CategoryAnalyticsResponse. */
    private Map<String, BigDecimal> toRevenueMap(CategoryAnalyticsResponse r) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (CategoryAnalyticsItem item : r.items()) {
            map.put(item.category(), item.totalRevenue());
        }
        return map;
    }

    /** Extracts categoryName → totalExpenses from a CategoryAnalyticsResponse. */
    private Map<String, BigDecimal> toExpenseMap(CategoryAnalyticsResponse r) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (CategoryAnalyticsItem item : r.items()) {
            map.put(item.category(), item.totalExpenses());
        }
        return map;
    }

    /** Extracts categoryName → totalProfit from a CategoryAnalyticsResponse. */
    private Map<String, BigDecimal> toProfitMap(CategoryAnalyticsResponse r) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (CategoryAnalyticsItem item : r.items()) {
            map.put(item.category(), item.totalProfit());
        }
        return map;
    }

    // ─── Category comparison between companies ───────────────────────────────

    public CategoryComparisonResponse getCategoryComparison(Long companyAId, Long companyBId,
                                                            String direction,
                                                            LocalDate start, LocalDate end) {
        String nameA = requireCompanyName(companyAId);
        String nameB = requireCompanyName(companyBId);

        com.bustech.erp.common.enums.FinancialDirection dir = resolveDirection(direction);

        CategoryBreakdownResponse breakdownA = companyDashboardService.getCategoryBreakdown(companyAId, start, end);
        CategoryBreakdownResponse breakdownB = companyDashboardService.getCategoryBreakdown(companyBId, start, end);

        List<CategoryBreakdownItem> listA = dir == com.bustech.erp.common.enums.FinancialDirection.INCOME
            ? breakdownA.income() : breakdownA.expense();
        List<CategoryBreakdownItem> listB = dir == com.bustech.erp.common.enums.FinancialDirection.INCOME
            ? breakdownB.income() : breakdownB.expense();

        Map<String, BigDecimal> mapA = toNameAmountMap(listA);
        Map<String, BigDecimal> mapB = toNameAmountMap(listB);

        // Merge all category names preserving A's order first, then B's extras
        Map<String, BigDecimal[]> merged = new LinkedHashMap<>();
        mapA.forEach((k, v) -> merged.computeIfAbsent(k, x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[0] = v);
        mapB.forEach((k, v) -> {
            merged.computeIfAbsent(k, x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[1] = v;
        });

        BigDecimal totalA = BigDecimal.ZERO;
        BigDecimal totalB = BigDecimal.ZERO;
        List<CategoryComparisonItem> items = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : merged.entrySet()) {
            BigDecimal a = e.getValue()[0];
            BigDecimal b = e.getValue()[1];
            totalA = totalA.add(a);
            totalB = totalB.add(b);
            items.add(new CategoryComparisonItem(e.getKey(), a, b, diffPct(a, b)));
        }
        items.sort((x, y) -> y.amountA().add(y.amountB()).compareTo(x.amountA().add(x.amountB())));

        return new CategoryComparisonResponse(nameA, nameB, dir.name(), items, totalA, totalB, diffPct(totalA, totalB));
    }

    // ─── Cost-Center Analytics Comparison (hybrid: rateio + direct FK) ──────

    /**
     * Per-cost-center revenue comparison between two companies.
     *
     * <p>Uses rateio cost-center allocations when available; falls back to the event's
     * primary cost-center FK. Cost centers present in only one company appear with 0
     * for the other. Sorted by combined magnitude descending.
     */
    public CostCenterAnalyticsComparisonResponse getCostCenterAnalyticsRevenue(
            Long companyAId, Long companyBId, LocalDate start, LocalDate end) {

        String nameA = requireCompanyName(companyAId);
        String nameB = requireCompanyName(companyBId);

        Map<String, BigDecimal> mapA = toRevenueMap(companyDashboardService.getCostCenterRevenue(companyAId, start, end));
        Map<String, BigDecimal> mapB = toRevenueMap(companyDashboardService.getCostCenterRevenue(companyBId, start, end));

        return buildCostCenterAnalyticsComparison(nameA, nameB, "REVENUE", mapA, mapB, start, end);
    }

    /**
     * Per-cost-center expense comparison between two companies.
     *
     * <p>Uses rateio cost-center allocations when available; falls back to the event's
     * primary cost-center FK. Cost centers present in only one company appear with 0
     * for the other. Sorted by combined magnitude descending.
     */
    public CostCenterAnalyticsComparisonResponse getCostCenterAnalyticsExpenses(
            Long companyAId, Long companyBId, LocalDate start, LocalDate end) {

        String nameA = requireCompanyName(companyAId);
        String nameB = requireCompanyName(companyBId);

        Map<String, BigDecimal> mapA = toExpensesMap(companyDashboardService.getCostCenterExpenses(companyAId, start, end));
        Map<String, BigDecimal> mapB = toExpensesMap(companyDashboardService.getCostCenterExpenses(companyBId, start, end));

        return buildCostCenterAnalyticsComparison(nameA, nameB, "EXPENSE", mapA, mapB, start, end);
    }

    /**
     * Per-cost-center summary comparison: revenue for each company in each cost center.
     *
     * <p>The "summary" metric for cost centers is revenue — the most useful view for
     * identifying business concentration. Uses the full hybrid query (rateio + direct FK).
     */
    public CostCenterAnalyticsComparisonResponse getCostCenterAnalyticsSummary(
            Long companyAId, Long companyBId, LocalDate start, LocalDate end) {

        String nameA = requireCompanyName(companyAId);
        String nameB = requireCompanyName(companyBId);

        // Summary: show revenue side (most useful for cost-center concentration analysis)
        Map<String, BigDecimal> mapA = toRevenueMap(companyDashboardService.getCostCenterAnalyticsSummary(companyAId, start, end));
        Map<String, BigDecimal> mapB = toRevenueMap(companyDashboardService.getCostCenterAnalyticsSummary(companyBId, start, end));

        return buildCostCenterAnalyticsComparison(nameA, nameB, "REVENUE", mapA, mapB, start, end);
    }

    /**
     * Merges two cost-center→amount maps into a {@link CostCenterAnalyticsComparisonResponse}.
     *
     * <p>All cost-center names from both companies are included. Items are sorted by
     * combined magnitude (|A| + |B|) descending.
     */
    private CostCenterAnalyticsComparisonResponse buildCostCenterAnalyticsComparison(
            String nameA, String nameB, String metric,
            Map<String, BigDecimal> mapA, Map<String, BigDecimal> mapB,
            LocalDate start, LocalDate end) {

        Map<String, BigDecimal[]> merged = new LinkedHashMap<>();
        mapA.forEach((k, v) ->
            merged.computeIfAbsent(k, x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[0] = v);
        mapB.forEach((k, v) ->
            merged.computeIfAbsent(k, x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[1] = v);

        BigDecimal totalA = BigDecimal.ZERO;
        BigDecimal totalB = BigDecimal.ZERO;
        List<CostCenterAnalyticsComparisonItem> items = new ArrayList<>();

        for (Map.Entry<String, BigDecimal[]> entry : merged.entrySet()) {
            BigDecimal a = entry.getValue()[0];
            BigDecimal b = entry.getValue()[1];
            totalA = totalA.add(a);
            totalB = totalB.add(b);
            items.add(new CostCenterAnalyticsComparisonItem(
                entry.getKey(), a, b, a.subtract(b), diffPct(a, b)));
        }

        items.sort((x, y) ->
            y.companyAValue().abs().add(y.companyBValue().abs())
             .compareTo(x.companyAValue().abs().add(x.companyBValue().abs())));

        BigDecimal diff = totalA.subtract(totalB);
        return new CostCenterAnalyticsComparisonResponse(
            start, end, nameA, nameB, metric, items,
            totalA, totalB, diff, diffPct(totalA, totalB));
    }

    // ─── Extraction helpers (cost-center) ────────────────────────────────────

    private Map<String, BigDecimal> toRevenueMap(CostCenterAnalyticsResponse r) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (CostCenterAnalyticsItem item : r.items()) {
            map.put(item.costCenterName(), item.totalRevenue());
        }
        return map;
    }

    private Map<String, BigDecimal> toExpensesMap(CostCenterAnalyticsResponse r) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (CostCenterAnalyticsItem item : r.items()) {
            map.put(item.costCenterName(), item.totalExpenses());
        }
        return map;
    }

    // ─── Cost-center comparison between companies ────────────────────────────

    public CostCenterComparisonResponse getCostCenterComparison(Long companyAId, Long companyBId,
                                                                String direction,
                                                                LocalDate start, LocalDate end) {
        String nameA = requireCompanyName(companyAId);
        String nameB = requireCompanyName(companyBId);

        com.bustech.erp.common.enums.FinancialDirection dir = resolveDirection(direction);

        CostCenterBreakdownResponse breakdownA = companyDashboardService.getCostCenterBreakdown(companyAId, start, end);
        CostCenterBreakdownResponse breakdownB = companyDashboardService.getCostCenterBreakdown(companyBId, start, end);

        List<CostCenterBreakdownItem> listA = dir == com.bustech.erp.common.enums.FinancialDirection.INCOME
            ? breakdownA.income() : breakdownA.expense();
        List<CostCenterBreakdownItem> listB = dir == com.bustech.erp.common.enums.FinancialDirection.INCOME
            ? breakdownB.income() : breakdownB.expense();

        Map<String, BigDecimal> mapA = toCostCenterNameAmountMap(listA);
        Map<String, BigDecimal> mapB = toCostCenterNameAmountMap(listB);

        Map<String, BigDecimal[]> merged = new LinkedHashMap<>();
        mapA.forEach((k, v) -> merged.computeIfAbsent(k, x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[0] = v);
        mapB.forEach((k, v) -> {
            merged.computeIfAbsent(k, x -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO})[1] = v;
        });

        BigDecimal totalA = BigDecimal.ZERO;
        BigDecimal totalB = BigDecimal.ZERO;
        List<CostCenterComparisonItem> items = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : merged.entrySet()) {
            BigDecimal a = e.getValue()[0];
            BigDecimal b = e.getValue()[1];
            totalA = totalA.add(a);
            totalB = totalB.add(b);
            items.add(new CostCenterComparisonItem(e.getKey(), a, b, diffPct(a, b)));
        }
        items.sort((x, y) -> y.amountA().add(y.amountB()).compareTo(x.amountA().add(x.amountB())));

        return new CostCenterComparisonResponse(nameA, nameB, dir.name(), items, totalA, totalB, diffPct(totalA, totalB));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private MonthlyComparisonResponse buildSeriesComparison(String nameA, String nameB,
                                                            List<MonthlySeriesPoint> seriesA,
                                                            List<MonthlySeriesPoint> seriesB) {
        List<MonthlyComparisonPoint> points = new ArrayList<>(seriesA.size());
        BigDecimal totalA = BigDecimal.ZERO;
        BigDecimal totalB = BigDecimal.ZERO;
        for (int i = 0; i < seriesA.size(); i++) {
            MonthlySeriesPoint pa = seriesA.get(i);
            MonthlySeriesPoint pb = seriesB.get(i);
            totalA = totalA.add(pa.amount());
            totalB = totalB.add(pb.amount());
            points.add(new MonthlyComparisonPoint(
                pa.year(), pa.month(), pa.monthLabel(),
                pa.amount(), pb.amount(), diffPct(pa.amount(), pb.amount())
            ));
        }
        return new MonthlyComparisonResponse(nameA, nameB, points, totalA, totalB, diffPct(totalA, totalB));
    }

    private CompanySnapshotDto toSnapshot(DashboardSummaryResponse s) {
        return new CompanySnapshotDto(
            s.companyId(), s.companyName(),
            s.totalRevenue(), s.totalExpense(), s.profit(), s.margin()
        );
    }

    private String requireCompanyName(Long companyId) {
        return companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Empresa", companyId))
            .getName();
    }

    private String winner(String nameA, String nameB, BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? nameA : nameB;
    }

    /**
     * Returns (A − B) / |B| × 100, rounded to 2 decimal places.
     * Positive means A > B. Returns 0 if both are zero; 100 if B is zero and A is positive.
     */
    private BigDecimal diffPct(BigDecimal a, BigDecimal b) {
        if (b.compareTo(BigDecimal.ZERO) == 0) {
            return a.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }
        return a.subtract(b)
            .divide(b.abs(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal computeMargin(BigDecimal revenue, BigDecimal profit) {
        if (revenue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return profit.divide(revenue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> toNameAmountMap(List<CategoryBreakdownItem> items) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (CategoryBreakdownItem item : items) {
            map.put(item.categoryName(), item.amount());
        }
        return map;
    }

    private Map<String, BigDecimal> toCostCenterNameAmountMap(List<CostCenterBreakdownItem> items) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        for (CostCenterBreakdownItem item : items) {
            map.put(item.costCenterName(), item.amount());
        }
        return map;
    }

    private com.bustech.erp.common.enums.FinancialDirection resolveDirection(String direction) {
        if (direction != null && direction.equalsIgnoreCase("INCOME")) {
            return com.bustech.erp.common.enums.FinancialDirection.INCOME;
        }
        return com.bustech.erp.common.enums.FinancialDirection.EXPENSE;
    }
}