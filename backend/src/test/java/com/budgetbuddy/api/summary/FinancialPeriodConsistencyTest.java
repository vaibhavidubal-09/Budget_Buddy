package com.budgetbuddy.api.summary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.budgetbuddy.api.assistant.AssistantFinancialContextService;
import com.budgetbuddy.api.budgets.BudgetRepository;
import com.budgetbuddy.api.expenses.ExpenseRepository;
import com.budgetbuddy.api.expenses.DatedCategoryExpenseTotal;
import com.budgetbuddy.api.income.IncomeRepository;
import com.budgetbuddy.api.time.FinancialPeriodCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialPeriodConsistencyTest {

    private static final Long USER_ID = 42L;

    @Mock private ExpenseRepository expenseRepository;
    @Mock private IncomeRepository incomeRepository;
    @Mock private BudgetRepository budgetRepository;

    private SummaryService summaryService;
    private AssistantFinancialContextService contextService;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        Clock clock = Clock.fixed(Instant.parse("2026-03-01T00:30:00Z"), zone);
        FinancialPeriodCalculator calculator = new FinancialPeriodCalculator(clock);
        summaryService = new SummaryService(expenseRepository, incomeRepository, budgetRepository, calculator);
        contextService = new AssistantFinancialContextService(expenseRepository, incomeRepository, budgetRepository,
                calculator);

        when(incomeRepository.findSchedulesRelevantToRange(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(budgetRepository.findAllByUserIdOrderByPeriodAscCategoryAsc(USER_ID)).thenReturn(List.of());
        when(expenseRepository.sumByDateAndCategoryAndUserIdBetween(eq(USER_ID), any(), any()))
                .thenReturn(List.<DatedCategoryExpenseTotal>of());
    }

    @Test
    void analyticsAndAssistantShareCurrentAndPreviousCalendarWindows() {
        Map<String, Object> context = contextService.buildContext(USER_ID);
        Map<?, ?> periods = (Map<?, ?>) context.get("periods");

        for (String period : List.of("weekly", "monthly", "yearly")) {
            PeriodSummary analytics = summaryService.getPeriodSummary(USER_ID, period);
            Map<?, ?> periodContext = (Map<?, ?>) periods.get(period);
            Map<?, ?> assistantCurrent = (Map<?, ?>) periodContext.get("current");
            Map<?, ?> assistantPrevious = (Map<?, ?>) periodContext.get("previous");
            PeriodTrend analyticsCurrent = analytics.trends().get(analytics.trends().size() - 1);
            PeriodTrend analyticsPrevious = analytics.trends().get(analytics.trends().size() - 2);
            LocalDate expectedPreviousEnd = switch (period) {
                case "weekly" -> analyticsPrevious.startDate().plusDays(6);
                case "yearly" -> LocalDate.of(analyticsPrevious.startDate().getYear(), 12, 31);
                default -> YearMonth.from(analyticsPrevious.startDate()).atEndOfMonth();
            };

            assertEquals(analytics.startDate().toString(), assistantCurrent.get("startDate"));
            assertEquals(analytics.endDate().toString(), assistantCurrent.get("endDate"));
            assertEquals(analyticsCurrent.startDate().toString(), assistantCurrent.get("startDate"));
            assertEquals(analyticsPrevious.startDate().toString(), assistantPrevious.get("startDate"));
            assertEquals(expectedPreviousEnd.toString(), assistantPrevious.get("endDate"));
        }

        org.mockito.Mockito.verify(incomeRepository).findSchedulesRelevantToRange(
                USER_ID, LocalDate.parse("2025-01-01"), LocalDate.parse("2026-12-31"));
        org.mockito.Mockito.verify(expenseRepository).sumByDateAndCategoryAndUserIdBetween(
                USER_ID, LocalDate.parse("2025-01-01"), LocalDate.parse("2026-12-31"));
        org.mockito.Mockito.verify(expenseRepository).sumByDateAndCategoryAndUserIdBetween(
                USER_ID, LocalDate.parse("2026-01-19"), LocalDate.parse("2026-03-01"));
        org.mockito.Mockito.verify(expenseRepository).sumByDateAndCategoryAndUserIdBetween(
                USER_ID, LocalDate.parse("2025-10-01"), LocalDate.parse("2026-03-31"));
        org.mockito.Mockito.verify(expenseRepository).sumByDateAndCategoryAndUserIdBetween(
                USER_ID, LocalDate.parse("2021-01-01"), LocalDate.parse("2026-12-31"));
    }
}
