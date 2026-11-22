package com.budgetbuddy.api.assistant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.budgetbuddy.api.budgets.Budget;
import com.budgetbuddy.api.budgets.BudgetRepository;
import com.budgetbuddy.api.expenses.DatedCategoryExpenseTotal;
import com.budgetbuddy.api.expenses.ExpenseRepository;
import com.budgetbuddy.api.income.IncomeEntry;
import com.budgetbuddy.api.income.IncomeRepository;
import com.budgetbuddy.api.time.FinancialPeriodCalculator;
import com.budgetbuddy.api.summary.ExpensePeriodAggregation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds the financial facts sent to the assistant from the signed-in user's stored records. */
@Service
public class AssistantFinancialContextService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;
    private final FinancialPeriodCalculator periodCalculator;

    public AssistantFinancialContextService(ExpenseRepository expenseRepository,
                                            IncomeRepository incomeRepository,
                                            BudgetRepository budgetRepository,
                                            FinancialPeriodCalculator periodCalculator) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.budgetRepository = budgetRepository;
        this.periodCalculator = periodCalculator;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildContext(Long userId) {
        LocalDate today = periodCalculator.today();
        FinancialPeriodCalculator.Window currentWeek = periodCalculator.window("weekly", 0, today);
        FinancialPeriodCalculator.Window previousWeek = periodCalculator.window("weekly", 1, today);
        FinancialPeriodCalculator.Window currentMonth = periodCalculator.window("monthly", 0, today);
        FinancialPeriodCalculator.Window previousMonth = periodCalculator.window("monthly", 1, today);
        FinancialPeriodCalculator.Window currentYear = periodCalculator.window("yearly", 0, today);
        FinancialPeriodCalculator.Window previousYear = periodCalculator.window("yearly", 1, today);
        LocalDate earliestStart = previousYear.start();
        LocalDate latestEnd = currentYear.end();
        List<IncomeEntry> incomeEntries = incomeRepository.findSchedulesRelevantToRange(userId, earliestStart, latestEnd);
        List<Budget> budgets = budgetRepository.findAllByUserIdOrderByPeriodAscCategoryAsc(userId);
        List<DatedCategoryExpenseTotal> expenseTotals = expenseRepository
                .sumByDateAndCategoryAndUserIdBetween(userId, earliestStart, latestEnd);

        Map<String, Object> periods = Map.of(
                "weekly", Map.of(
                        "current", summarize("weekly", currentWeek.start(), currentWeek.end(), incomeEntries, budgets, expenseTotals),
                        "previous", summarize("weekly", previousWeek.start(), previousWeek.end(), incomeEntries, budgets, expenseTotals)),
                "monthly", Map.of(
                        "current", summarize("monthly", currentMonth.start(), currentMonth.end(), incomeEntries, budgets, expenseTotals),
                        "previous", summarize("monthly", previousMonth.start(), previousMonth.end(), incomeEntries, budgets, expenseTotals)),
                "yearly", Map.of(
                        "current", summarize("yearly", currentYear.start(), currentYear.end(), incomeEntries, budgets, expenseTotals),
                        "previous", summarize("yearly", previousYear.start(), previousYear.end(), incomeEntries, budgets, expenseTotals)));

        return Map.of("asOfDate", today.toString(), "periods", periods);
    }

    private Map<String, Object> summarize(String period, LocalDate start, LocalDate end,
                                          List<IncomeEntry> incomeEntries, List<Budget> budgets,
                                          List<DatedCategoryExpenseTotal> expenseTotals) {
        Map<String, BigDecimal> totalsByCategory = ExpensePeriodAggregation.byCategory(expenseTotals, start, end);
        BigDecimal expenseTotal = totalsByCategory.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> categories = totalsByCategory.entrySet().stream()
                .map(entry -> Map.<String, Object>of("category", entry.getKey(), "amount", entry.getValue()))
                .toList();

        List<Map<String, Object>> budgetStatus = new ArrayList<>();
        for (Budget budget : budgets) {
            if (!period.equalsIgnoreCase(budget.getPeriod())) continue;
            BigDecimal limit = BigDecimal.valueOf(budget.getLimit());
            BigDecimal spent = totalsByCategory.getOrDefault(budget.getCategory(), BigDecimal.ZERO);
            BigDecimal remaining = limit.subtract(spent);
            budgetStatus.add(Map.of(
                    "category", budget.getCategory(),
                    "limit", limit,
                    "spent", spent,
                    "remaining", remaining,
                    "overBudget", remaining.signum() < 0));
        }

        BigDecimal scheduledIncome = incomeEntries.stream()
                .map(entry -> entry.getAmount().multiply(BigDecimal.valueOf(countOccurrences(entry, start, end))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "startDate", start.toString(),
                "endDate", end.toString(),
                "scheduledIncomeEstimate", scheduledIncome,
                "expenseTotal", expenseTotal,
                "remainingAfterExpenses", scheduledIncome.subtract(expenseTotal),
                "spendingByCategory", categories,
                "budgets", budgetStatus);
    }

    private long countOccurrences(IncomeEntry entry, LocalDate start, LocalDate end) {
        LocalDate first = entry.getDate();
        if (first.isAfter(end)) return 0;
        String frequency = entry.getFrequency() == null ? "ONCE" : entry.getFrequency();
        if ("ONCE".equals(frequency)) return inRange(first, start, end) ? 1 : 0;

        if ("WEEKLY".equals(frequency) || "CUSTOM".equals(frequency)) {
            int interval = "WEEKLY".equals(frequency) ? 7 : Math.max(1, entry.getIntervalDays() == null ? 1 : entry.getIntervalDays());
            long daysUntilStart = Math.max(0, ChronoUnit.DAYS.between(first, start));
            long steps = (daysUntilStart + interval - 1) / interval;
            LocalDate occurrence = first.plusDays(steps * interval);
            long count = 0;
            while (!occurrence.isAfter(end)) {
                if (!occurrence.isBefore(start)) count++;
                occurrence = occurrence.plusDays(interval);
            }
            return count;
        }

        if ("MONTHLY".equals(frequency)) {
            YearMonth month = YearMonth.from(start);
            YearMonth lastMonth = YearMonth.from(end);
            long count = 0;
            while (!month.isAfter(lastMonth)) {
                LocalDate occurrence = month.atDay(Math.min(first.getDayOfMonth(), month.lengthOfMonth()));
                if (!occurrence.isBefore(first) && inRange(occurrence, start, end)) count++;
                month = month.plusMonths(1);
            }
            return count;
        }

        if ("YEARLY".equals(frequency)) {
            long count = 0;
            for (int year = Math.max(first.getYear(), start.getYear()); year <= end.getYear(); year++) {
                int day = Math.min(first.getDayOfMonth(), first.getMonth().length(java.time.Year.isLeap(year)));
                LocalDate occurrence = LocalDate.of(year, first.getMonth(), day);
                if (!occurrence.isBefore(first) && inRange(occurrence, start, end)) count++;
            }
            return count;
        }

        return inRange(first, start, end) ? 1 : 0;
    }

    private boolean inRange(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
