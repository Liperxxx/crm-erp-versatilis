package com.bustech.erp.dashboard.service;

import com.bustech.erp.common.enums.FinancialDirection;
import com.bustech.erp.common.enums.TransactionStatus;
import com.bustech.erp.company.entity.Company;
import com.bustech.erp.company.repository.CompanyRepository;
import com.bustech.erp.dashboard.dto.*;
import com.bustech.erp.financial.repository.FinancialEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final FinancialEventRepository eventRepository;
    private final CompanyRepository companyRepository;

    public DashboardResponse getDashboard(Long companyId, int year) {
        Company company = companyRepository.findById(Objects.requireNonNull(companyId))
            .orElseThrow(() -> new com.bustech.erp.common.exception.ResourceNotFoundException("Empresa", companyId));

        LocalDate today = LocalDate.now();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        BigDecimal totalIncome = eventRepository.sumPaidByCompanyAndDirectionAndPeriod(
            companyId, FinancialDirection.INCOME, yearStart, yearEnd);
        BigDecimal totalExpense = eventRepository.sumPaidByCompanyAndDirectionAndPeriod(
            companyId, FinancialDirection.EXPENSE, yearStart, yearEnd);
        BigDecimal overdueAmount = eventRepository.sumOverdueByCompanyId(companyId, today);

        long pendingCount = eventRepository.findByCompanyIdAndStatus(
            companyId, TransactionStatus.PENDING, Pageable.unpaged()).getTotalElements();
        long overdueCount = eventRepository.findByCompanyIdAndStatus(
            companyId, TransactionStatus.OVERDUE, Pageable.unpaged()).getTotalElements();

        CompanySummaryDto summary = new CompanySummaryDto(
            companyId,
            company.getName(),
            totalIncome,
            totalExpense,
            totalIncome.subtract(totalExpense),
            overdueAmount,
            pendingCount,
            overdueCount
        );

        return new DashboardResponse(summary, buildMonthlySeries(companyId, year, FinancialDirection.INCOME),
            buildMonthlySeries(companyId, year, FinancialDirection.EXPENSE));
    }

    public ComparativeDashboardResponse getComparativeDashboard(int year) {
        List<Company> companies = companyRepository.findAll();
        List<CompanySummaryDto> summaries = new ArrayList<>();

        BigDecimal consolidatedIncome = BigDecimal.ZERO;
        BigDecimal consolidatedExpense = BigDecimal.ZERO;
        BigDecimal consolidatedOverdue = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        for (Company company : companies) {
            BigDecimal income = eventRepository.sumPaidByCompanyAndDirectionAndPeriod(
                company.getId(), FinancialDirection.INCOME, yearStart, yearEnd);
            BigDecimal expense = eventRepository.sumPaidByCompanyAndDirectionAndPeriod(
                company.getId(), FinancialDirection.EXPENSE, yearStart, yearEnd);
            BigDecimal overdue = eventRepository.sumOverdueByCompanyId(company.getId(), today);

            long pendingCount = eventRepository.findByCompanyIdAndStatus(
                company.getId(), TransactionStatus.PENDING, Pageable.unpaged()).getTotalElements();
            long overdueCount = eventRepository.findByCompanyIdAndStatus(
                company.getId(), TransactionStatus.OVERDUE, Pageable.unpaged()).getTotalElements();

            summaries.add(new CompanySummaryDto(
                company.getId(), company.getName(),
                income, expense, income.subtract(expense),
                overdue, pendingCount, overdueCount
            ));

            consolidatedIncome = consolidatedIncome.add(income);
            consolidatedExpense = consolidatedExpense.add(expense);
            consolidatedOverdue = consolidatedOverdue.add(overdue);
        }

        ConsolidatedSummaryDto consolidated = new ConsolidatedSummaryDto(
            consolidatedIncome, consolidatedExpense,
            consolidatedIncome.subtract(consolidatedExpense), consolidatedOverdue
        );

        return new ComparativeDashboardResponse(summaries, consolidated);
    }

    private List<MonthlySeriesDto> buildMonthlySeries(Long companyId, int year, FinancialDirection direction) {
        List<MonthlySeriesDto> series = new ArrayList<>();
        List<Object[]> rows = eventRepository.findMonthlyPaidSums(
            companyId,
            direction,
            LocalDate.of(year, 1, 1),
            LocalDate.of(year, 12, 31)
        );
        for (int m = 1; m <= 12; m++) {
            BigDecimal amount = BigDecimal.ZERO;
            for (Object[] row : rows) {
                int rowYear = ((Number) row[0]).intValue();
                int rowMonth = ((Number) row[1]).intValue();
                if (rowYear == year && rowMonth == m) {
                    amount = (BigDecimal) row[2];
                    break;
                }
            }
            String label = Month.of(m).getDisplayName(TextStyle.SHORT, Locale.of("pt", "BR"));
            series.add(new MonthlySeriesDto(year, m, label, amount));
        }
        return series;
    }
}
