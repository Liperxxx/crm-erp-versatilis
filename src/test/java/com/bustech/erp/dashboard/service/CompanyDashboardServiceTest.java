package com.bustech.erp.dashboard.service;

import com.bustech.erp.common.enums.FinancialDirection;
import com.bustech.erp.common.enums.TransactionStatus;
import com.bustech.erp.common.exception.ResourceNotFoundException;
import com.bustech.erp.company.entity.Company;
import com.bustech.erp.company.repository.CompanyRepository;
import com.bustech.erp.financial.repository.FinancialEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyDashboardServiceTest {

    @Mock FinancialEventRepository eventRepository;
    @Mock CompanyRepository        companyRepository;

    @InjectMocks
    CompanyDashboardService dashboardService;

    // ─── getSummary ───────────────────────────────────────────────────────────

    @Test
    void getSummary_computesProfitAndMarginCorrectly() {
        var companyId = 1L;
        var start = LocalDate.of(2025, 1, 1);
        var end   = LocalDate.of(2025, 12, 31);

        stubCompany(companyId);
        when(eventRepository.sumPaidByCompanyAndDirectionAndPeriod(eq(companyId), eq(FinancialDirection.INCOME),  any(), any()))
            .thenReturn(new BigDecimal("10000.00"));
        when(eventRepository.sumPaidByCompanyAndDirectionAndPeriod(eq(companyId), eq(FinancialDirection.EXPENSE), any(), any()))
            .thenReturn(new BigDecimal("6000.00"));
        when(eventRepository.sumOverdueByCompanyId(eq(companyId), any())).thenReturn(BigDecimal.ZERO);
        when(eventRepository.countPendingByCompanyId(eq(companyId), any())).thenReturn(3L);
        when(eventRepository.findByCompanyIdAndStatus(eq(companyId), eq(TransactionStatus.OVERDUE), any()))
            .thenReturn(new PageImpl<>(List.of()));

        var result = dashboardService.getSummary(companyId, start, end);

        assertThat(result.totalRevenue()).isEqualByComparingTo("10000.00");
        assertThat(result.totalExpense()).isEqualByComparingTo("6000.00");
        assertThat(result.profit()).isEqualByComparingTo("4000.00");
        assertThat(result.margin()).isEqualByComparingTo("40.00");
        assertThat(result.pendingCount()).isEqualTo(3L);
        assertThat(result.companyName()).isEqualTo("Bustech");
    }

    @Test
    void getSummary_zeroRevenue_marginIsZero() {
        stubCompany(1L);
        when(eventRepository.sumPaidByCompanyAndDirectionAndPeriod(eq(1L), eq(FinancialDirection.INCOME),  any(), any()))
            .thenReturn(BigDecimal.ZERO);
        when(eventRepository.sumPaidByCompanyAndDirectionAndPeriod(eq(1L), eq(FinancialDirection.EXPENSE), any(), any()))
            .thenReturn(BigDecimal.ZERO);
        when(eventRepository.sumOverdueByCompanyId(eq(1L), any())).thenReturn(BigDecimal.ZERO);
        when(eventRepository.countPendingByCompanyId(eq(1L), any())).thenReturn(0L);
        when(eventRepository.findByCompanyIdAndStatus(any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of()));

        var result = dashboardService.getSummary(1L, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31));

        assertThat(result.margin()).isEqualByComparingTo("0");
    }

    @Test
    void getSummary_unknownCompany_throwsResourceNotFoundException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getSummary(99L,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── getMonthlyRevenue ────────────────────────────────────────────────────

    @Test
    void getMonthlyRevenue_fillsZerosForMonthsWithNoData() {
        var companyId = 1L;
        var start = LocalDate.of(2025, 1, 1);
        var end   = LocalDate.of(2025, 3, 31);

        stubCompany(companyId);
        // Only January has recorded revenue
        when(eventRepository.findMonthlyPaidSums(eq(companyId), eq(FinancialDirection.INCOME), eq(start), eq(end)))
            .thenReturn(List.<Object[]>of(row(2025, 1, "5000.00")));

        var result = dashboardService.getMonthlyRevenue(companyId, start, end);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).amount()).isEqualByComparingTo("5000.00"); // Jan
        assertThat(result.get(1).amount()).isEqualByComparingTo("0");       // Feb — zero filled
        assertThat(result.get(2).amount()).isEqualByComparingTo("0");       // Mar — zero filled
    }

    @Test
    void getMonthlyRevenue_monthLabelIsPortuguese() {
        var companyId = 1L;
        var start = LocalDate.of(2025, 1, 1);
        var end   = LocalDate.of(2025, 1, 31);

        stubCompany(companyId);
        when(eventRepository.findMonthlyPaidSums(any(), any(), any(), any())).thenReturn(List.of());

        var result = dashboardService.getMonthlyRevenue(companyId, start, end);

        // Month label: Portuguese short name (pt-BR adds trailing dot) + /YY
        assertThat(result.get(0).monthLabel()).startsWith("jan").endsWith("/25");
    }

    // ─── getMonthlyProfit ─────────────────────────────────────────────────────

    @Test
    void getMonthlyProfit_subtractsExpenseFromRevenue() {
        var companyId = 1L;
        var start = LocalDate.of(2025, 1, 1);
        var end   = LocalDate.of(2025, 2, 28);

        stubCompany(companyId);
        when(eventRepository.findMonthlyPaidSums(eq(companyId), eq(FinancialDirection.INCOME),  eq(start), eq(end)))
            .thenReturn(List.<Object[]>of(row(2025, 1, "8000"), row(2025, 2, "7000")));
        when(eventRepository.findMonthlyPaidSums(eq(companyId), eq(FinancialDirection.EXPENSE), eq(start), eq(end)))
            .thenReturn(List.<Object[]>of(row(2025, 1, "3000")));

        var result = dashboardService.getMonthlyProfit(companyId, start, end);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).revenue()).isEqualByComparingTo("8000");
        assertThat(result.get(0).expense()).isEqualByComparingTo("3000");
        assertThat(result.get(0).profit()).isEqualByComparingTo("5000");
        // Feb: expense absent → zero
        assertThat(result.get(1).expense()).isEqualByComparingTo("0");
        assertThat(result.get(1).profit()).isEqualByComparingTo("7000");
    }

    // ─── getMonthlyExpenses ───────────────────────────────────────────────────

    @Test
    void getMonthlyExpenses_returnsExpenseSeries() {
        var companyId = 1L;
        var start = LocalDate.of(2025, 6, 1);
        var end   = LocalDate.of(2025, 6, 30);

        stubCompany(companyId);
        when(eventRepository.findMonthlyPaidSums(eq(companyId), eq(FinancialDirection.EXPENSE), eq(start), eq(end)))
            .thenReturn(List.<Object[]>of(row(2025, 6, "2500")));

        var result = dashboardService.getMonthlyExpenses(companyId, start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).amount()).isEqualByComparingTo("2500");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void stubCompany(Long id) {
        when(companyRepository.findById(id))
            .thenReturn(Optional.of(Company.builder()
                .id(id).name("Bustech").slug("bustech").active(true)
                .createdAt(Instant.EPOCH).updatedAt(Instant.EPOCH)
                .build()));
    }

    /** Builds an Object[] {year, month, amount} as returned by findMonthlyPaidSums. */
    private static Object[] row(int year, int month, String amount) {
        return new Object[]{year, month, new BigDecimal(amount)};
    }
}