package com.budgetbuddy.api.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import com.budgetbuddy.api.budgets.Budget;
import com.budgetbuddy.api.budgets.BudgetRepository;
import com.budgetbuddy.api.expenses.ExpenseRepository;
import com.budgetbuddy.api.expenses.DatedCategoryExpenseTotal;
import com.budgetbuddy.api.income.IncomeEntry;
import com.budgetbuddy.api.income.IncomeRepository;
import com.budgetbuddy.api.time.FinancialPeriodCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssistantFinancialContextServiceTest {

    private static final Long USER_ID = 42L;
    private static final LocalDate TODAY = LocalDate.parse("2026-10-14");

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private IncomeRepository incomeRepository;

    @Mock
    private BudgetRepository budgetRepository;

    private AssistantFinancialContextService contextService;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.of("Asia/Kolkata");
        Clock clock = Clock.fixed(TODAY.atTime(12, 0).atZone(zone).toInstant(), zone);
        contextService = new AssistantFinancialContextService(expenseRepository, incomeRepository, budgetRepository,
                new FinancialPeriodCalculator(clock));
    }

    @Test
    void buildsPeriodTotalsFromOnlyTheAuthenticatedUsersDatabaseRecords() {
        when(expenseRepository.sumByDateAndCategoryAndUserIdBetween(USER_ID,
                LocalDate.parse("2025-01-01"), LocalDate.parse("2026-12-31"))).thenReturn(List.of(
                aggregate("Food & Dining", "80.00", "2026-10-14"),
                aggregate("Transportation", "50.00", "2026-10-09"),
                aggregate("Food & Dining", "30.00", "2026-10-07"),
                aggregate("Education", "90.00", "2026-10-03"),
                aggregate("Food & Dining", "30.00", "2026-09-07"),
                aggregate("Food & Dining", "30.00", "2025-10-07")));

        when(incomeRepository.findSchedulesRelevantToRange(USER_ID,
                LocalDate.parse("2025-01-01"), LocalDate.parse("2026-12-31"))).thenReturn(List.of(
                new IncomeEntry(USER_ID, "Allowance", new BigDecimal("1000.00"), LocalDate.parse("2026-10-10"), "ONCE", null),
                new IncomeEntry(USER_ID, "Scholarship", new BigDecimal("500.00"), LocalDate.parse("2026-09-05"), "MONTHLY", null)));
        when(budgetRepository.findAllByUserIdOrderByPeriodAscCategoryAsc(USER_ID)).thenReturn(List.of(
                new Budget(USER_ID, "Food & Dining", 100, "weekly"),
                new Budget(USER_ID, "Food & Dining", 200, "monthly")));

        Map<String, Object> context = contextService.buildContext(USER_ID);

        Map<?, ?> periods = map(context.get("periods"));
        Map<?, ?> monthly = map(periods.get("monthly"));
        Map<?, ?> current = map(monthly.get("current"));
        Map<?, ?> previous = map(monthly.get("previous"));

        assertEquals("2026-10-14", context.get("asOfDate"));
        assertEquals(new BigDecimal("250.00"), current.get("expenseTotal"));
        assertEquals(new BigDecimal("1500.00"), current.get("scheduledIncomeEstimate"));
        assertEquals(new BigDecimal("30.00"), previous.get("expenseTotal"));
        assertEquals(new BigDecimal("500.00"), previous.get("scheduledIncomeEstimate"));

        Map<?, ?> food = map(((List<?>) current.get("budgets")).get(0));
        assertEquals(new BigDecimal("110.00"), food.get("spent"));
        assertEquals(new BigDecimal("90.00"), food.get("remaining"));
        assertEquals(false, food.get("overBudget"));

        verify(expenseRepository).sumByDateAndCategoryAndUserIdBetween(
                USER_ID, LocalDate.parse("2025-01-01"), LocalDate.parse("2026-12-31"));
        verify(incomeRepository).findSchedulesRelevantToRange(
                USER_ID, LocalDate.parse("2025-01-01"), LocalDate.parse("2026-12-31"));
        verify(budgetRepository).findAllByUserIdOrderByPeriodAscCategoryAsc(USER_ID);
    }

    private DatedCategoryExpenseTotal aggregate(String category, String amount, String date) {
        return new DatedCategoryExpenseTotal(LocalDate.parse(date), category, new BigDecimal(amount));
    }

    private Map<?, ?> map(Object value) {
        return (Map<?, ?>) value;
    }
}
