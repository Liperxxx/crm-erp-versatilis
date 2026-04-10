package com.bustech.erp.dashboard.service;

import com.bustech.erp.common.enums.FinancialDirection;
import com.bustech.erp.company.repository.CompanyRepository;
import com.bustech.erp.dashboard.dto.*;
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
public class ConsolidatedDashboardService {

    private final FinancialEventRepository eventRepository;
    private final CompanyRepository companyRepository;

    // ─── Summary ─────────────────────────────────────────────────────────────

    public ConsolidatedSummaryResponse getSummary(LocalDate start, LocalDate end) {
        List<Long> ids = allCompanyIds();

        BigDecimal revenue  = eventRepository.sumPaidByCompanyIdsAndDirectionAndPeriod(ids, FinancialDirection.INCOME,  start, end);
        BigDecimal expenses = eventRepository.sumPaidByCompanyIdsAndDirectionAndPeriod(ids, FinancialDirection.EXPENSE, start, end);
        BigDecimal profit   = revenue.subtract(expenses);
        BigDecimal margin   = computeMargin(revenue, profit);

        List<CompanyRevenueShareDto> shares = buildRevenueShares(ids, revenue, start, end);

        return new ConsolidatedSummaryResponse(start, end, revenue, expenses, profit, margin, shares);
    }

    // ─── Monthly Results ─────────────────────────────────────────────────────

    public ConsolidatedMonthlyResponse getMonthlyResults(LocalDate start, LocalDate end) {
        List<Long> ids = allCompanyIds();

        // Single query: (companyId, companyName, year, month, direction, sum)
        List<Object[]> rows = eventRepository.findMonthlyPerCompanyCashFlow(ids, start, end);

        // Index: "companyId|year|month|INCOME" → amount
        // and   "year|month" → BigDecimal[2]{income, expense}
        Map<String, BigDecimal> cellMap = new LinkedHashMap<>();  // "cId|y|m|DIR" → amount
        for (Object[] row : rows) {
            long   cId    = ((Number) row[0]).longValue();
            int    year   = ((Number) row[2]).intValue();
            int    month  = ((Number) row[3]).intValue();
            FinancialDirection dir = (FinancialDirection) row[4];
            BigDecimal amt = (BigDecimal) row[5];
            cellMap.put(cId + "|" + year + "|" + month + "|" + dir.name(), amt);
        }

        // Collect per-company references: id → name
        Map<Long, String> companyNames = new LinkedHashMap<>();
        for (Object[] row : rows) {
            long cId = ((Number) row[0]).longValue();
            companyNames.putIfAbsent(cId, (String) row[1]);
        }

        BigDecimal grandRevenue  = BigDecimal.ZERO;
        BigDecimal grandExpenses = BigDecimal.ZERO;
        List<ConsolidatedMonthlyPoint> points = new ArrayList<>();

        for (YearMonth ym : monthRange(start, end)) {
            int y = ym.getYear(), m = ym.getMonthValue();
            BigDecimal consRevenue = BigDecimal.ZERO;
            BigDecimal consExpense = BigDecimal.ZERO;
            List<CompanyMonthEntryDto> byCompany = new ArrayList<>(companyNames.size());

            for (Map.Entry<Long, String> company : companyNames.entrySet()) {
                long cId  = company.getKey();
                String cn = company.getValue();
                BigDecimal rev = cellMap.getOrDefault(cId + "|" + y + "|" + m + "|INCOME",  BigDecimal.ZERO);
                BigDecimal exp = cellMap.getOrDefault(cId + "|" + y + "|" + m + "|EXPENSE", BigDecimal.ZERO);
                byCompany.add(new CompanyMonthEntryDto(cId, cn, rev, exp, rev.subtract(exp)));
                consRevenue = consRevenue.add(rev);
                consExpense = consExpense.add(exp);
            }

            grandRevenue  = grandRevenue.add(consRevenue);
            grandExpenses = grandExpenses.add(consExpense);
            points.add(new ConsolidatedMonthlyPoint(
                y, m, monthLabel(ym),
                consRevenue, consExpense, consRevenue.subtract(consExpense),
                byCompany
            ));
        }

        // Period-level revenue share (reuse dedicated query for accuracy)
        List<CompanyRevenueShareDto> shares = buildRevenueShares(ids, grandRevenue, start, end);

        return new ConsolidatedMonthlyResponse(
            points, grandRevenue, grandExpenses,
            grandRevenue.subtract(grandExpenses), shares
        );
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private List<Long> allCompanyIds() {
        return companyRepository.findAll().stream()
            .map(c -> c.getId())
            .toList();
    }

    private List<CompanyRevenueShareDto> buildRevenueShares(List<Long> ids, BigDecimal totalRevenue,
                                                             LocalDate from, LocalDate to) {
        List<Object[]> rows = eventRepository.findRevenueSumPerCompanyIds(ids, from, to);
        return rows.stream().map(row -> {
            long   cId  = ((Number) row[0]).longValue();
            String name = (String) row[1];
            BigDecimal rev = (BigDecimal) row[2];
            BigDecimal pct = totalRevenue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : rev.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                     .multiply(BigDecimal.valueOf(100))
                     .setScale(2, RoundingMode.HALF_UP);
            return new CompanyRevenueShareDto(cId, name, rev, pct);
        }).toList();
    }

    private BigDecimal computeMargin(BigDecimal revenue, BigDecimal profit) {
        if (revenue.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return profit.divide(revenue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
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
}