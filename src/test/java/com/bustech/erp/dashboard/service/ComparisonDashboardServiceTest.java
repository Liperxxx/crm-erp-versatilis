package com.bustech.erp.dashboard.service;

import com.bustech.erp.company.entity.Company;
import com.bustech.erp.company.repository.CompanyRepository;
import com.bustech.erp.dashboard.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComparisonDashboardServiceTest {

    @Mock CompanyDashboardService companyDashboardService;
    @Mock CompanyRepository       companyRepository;

    @InjectMocks
    ComparisonDashboardService comparisonService;

    private static final LocalDate START = LocalDate.of(2025, 1, 1);
    private static final LocalDate END   = LocalDate.of(2025, 12, 31);

    // ─── getSummary ───────────────────────────────────────────────────────────

    @Test
    void getSummary_identifiesHigherRevenueCompany() {
        when(companyDashboardService.getSummary(1L, START, END))
            .thenReturn(summary(1L, "Bustech",    bd("20000"), bd("12000"), bd("8000"),  bd("40.00")));
        when(companyDashboardService.getSummary(2L, START, END))
            .thenReturn(summary(2L, "Versatilis", bd("15000"), bd("8000"),  bd("7000"),  bd("46.67")));

        var result = comparisonService.getSummary(1L, 2L, START, END);

        assertThat(result.higherRevenue()).isEqualTo("Bustech");
        assertThat(result.higherMargin()).isEqualTo("Versatilis");
    }

    @Test
    void getSummary_revenueDiffPct_positiveWhenAGreaterThanB() {
        // A=20000, B=10000 → diff = +100%
        when(companyDashboardService.getSummary(1L, START, END))
            .thenReturn(summary(1L, "Bustech",    bd("20000"), bd("5000"), bd("15000"), bd("75.00")));
        when(companyDashboardService.getSummary(2L, START, END))
            .thenReturn(summary(2L, "Versatilis", bd("10000"), bd("5000"), bd("5000"),  bd("50.00")));

        var result = comparisonService.getSummary(1L, 2L, START, END);

        assertThat(result.revenueDiffPct()).isEqualByComparingTo("100.00");
    }

    @Test
    void getSummary_revenueDiffPct_negativeWhenBGreaterThanA() {
        // A=8000, B=10000 → diff = -20%
        when(companyDashboardService.getSummary(1L, START, END))
            .thenReturn(summary(1L, "Bustech",    bd("8000"),  bd("5000"), bd("3000"), bd("37.50")));
        when(companyDashboardService.getSummary(2L, START, END))
            .thenReturn(summary(2L, "Versatilis", bd("10000"), bd("5000"), bd("5000"), bd("50.00")));

        var result = comparisonService.getSummary(1L, 2L, START, END);

        assertThat(result.revenueDiffPct()).isNegative();
    }

    @Test
    void getSummary_bothZeroRevenue_diffPctIsZero() {
        when(companyDashboardService.getSummary(1L, START, END))
            .thenReturn(summary(1L, "Bustech",    bd("0"), bd("0"), bd("0"), bd("0")));
        when(companyDashboardService.getSummary(2L, START, END))
            .thenReturn(summary(2L, "Versatilis", bd("0"), bd("0"), bd("0"), bd("0")));

        var result = comparisonService.getSummary(1L, 2L, START, END);

        assertThat(result.revenueDiffPct()).isEqualByComparingTo("0");
    }

    // ─── getMonthlyRevenue ────────────────────────────────────────────────────

    @Test
    void getMonthlyRevenue_zipsSeriesAndComputesTotals() {
        stubCompanyName(1L, "Bustech");
        stubCompanyName(2L, "Versatilis");

        when(companyDashboardService.getMonthlyRevenue(1L, START, END)).thenReturn(List.of(
            point(2025, 1, "jan/25", "8000"),
            point(2025, 2, "fev/25", "9000")
        ));
        when(companyDashboardService.getMonthlyRevenue(2L, START, END)).thenReturn(List.of(
            point(2025, 1, "jan/25", "6000"),
            point(2025, 2, "fev/25", "7000")
        ));

        var result = comparisonService.getMonthlyRevenue(1L, 2L, START, END);

        assertThat(result.companyAName()).isEqualTo("Bustech");
        assertThat(result.companyBName()).isEqualTo("Versatilis");
        assertThat(result.points()).hasSize(2);
        assertThat(result.points().get(0).valueA()).isEqualByComparingTo("8000");
        assertThat(result.points().get(0).valueB()).isEqualByComparingTo("6000");
        assertThat(result.totalA()).isEqualByComparingTo("17000");
        assertThat(result.totalB()).isEqualByComparingTo("13000");
    }

    @Test
    void getMonthlyRevenue_diffPctPerPointReflectsDifference() {
        stubCompanyName(1L, "Bustech");
        stubCompanyName(2L, "Versatilis");

        // A=10000, B=5000 → diff = +100%
        when(companyDashboardService.getMonthlyRevenue(1L, START, END))
            .thenReturn(List.of(point(2025, 1, "jan/25", "10000")));
        when(companyDashboardService.getMonthlyRevenue(2L, START, END))
            .thenReturn(List.of(point(2025, 1, "jan/25", "5000")));

        var result = comparisonService.getMonthlyRevenue(1L, 2L, START, END);

        assertThat(result.points().get(0).diffPct()).isEqualByComparingTo("100.00");
    }

    // ─── getMonthlyResults ────────────────────────────────────────────────────

    @Test
    void getMonthlyResults_mergesProfit_forBothCompanies() {
        stubCompanyName(1L, "Bustech");
        stubCompanyName(2L, "Versatilis");

        when(companyDashboardService.getMonthlyProfit(1L, START, END)).thenReturn(List.of(
            profitPoint(2025, 1, "jan/25", "8000", "3000", "5000")
        ));
        when(companyDashboardService.getMonthlyProfit(2L, START, END)).thenReturn(List.of(
            profitPoint(2025, 1, "jan/25", "6000", "2000", "4000")
        ));

        var result = comparisonService.getMonthlyResults(1L, 2L, START, END);

        assertThat(result.points()).hasSize(1);
        var p = result.points().get(0);
        assertThat(p.revenueA()).isEqualByComparingTo("8000");
        assertThat(p.profitA()).isEqualByComparingTo("5000");
        assertThat(p.revenueB()).isEqualByComparingTo("6000");
        assertThat(p.profitB()).isEqualByComparingTo("4000");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void stubCompanyName(Long id, String name) {
        when(companyRepository.findById(id))
            .thenReturn(Optional.of(Company.builder()
                .id(id).name(name).slug(name.toLowerCase()).active(true)
                .createdAt(Instant.EPOCH).updatedAt(Instant.EPOCH)
                .build()));
    }

    private static DashboardSummaryResponse summary(Long id, String name,
                                                     BigDecimal rev, BigDecimal exp,
                                                     BigDecimal profit, BigDecimal margin) {
        return new DashboardSummaryResponse(id, name,
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
            rev, exp, profit, margin,
            profit, BigDecimal.ZERO, 0L, 0L);
    }

    private static MonthlySeriesPoint point(int year, int month, String label, String amount) {
        return new MonthlySeriesPoint(year, month, label, new BigDecimal(amount));
    }

    private static MonthlyProfitPoint profitPoint(int year, int month, String label,
                                                   String rev, String exp, String profit) {
        return new MonthlyProfitPoint(year, month, label,
            new BigDecimal(rev), new BigDecimal(exp), new BigDecimal(profit));
    }

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}