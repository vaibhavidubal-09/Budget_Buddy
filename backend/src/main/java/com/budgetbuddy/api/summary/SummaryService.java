package com.budgetbuddy.api.summary;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import com.budgetbuddy.api.budgets.BudgetRepository;
import com.budgetbuddy.api.expenses.ExpenseRepository;
import com.budgetbuddy.api.expenses.DatedCategoryExpenseTotal;
import com.budgetbuddy.api.income.IncomeEntry;
import com.budgetbuddy.api.income.IncomeRepository;
import com.budgetbuddy.api.time.FinancialPeriodCalculator;
import org.springframework.stereotype.Service;

@Service
public class SummaryService {

    private final ExpenseRepository expenseRepository;
    private final IncomeRepository incomeRepository;
    private final BudgetRepository budgetRepository;
    private final FinancialPeriodCalculator periodCalculator;

    public SummaryService(ExpenseRepository expenseRepository,
                          IncomeRepository incomeRepository,
                          BudgetRepository budgetRepository,
                          FinancialPeriodCalculator periodCalculator) {
        this.expenseRepository = expenseRepository;
        this.incomeRepository = incomeRepository;
        this.budgetRepository = budgetRepository;
        this.periodCalculator = periodCalculator;
    }

    public MonthlySummary getCurrentMonthSummary(Long userId) {
        YearMonth currentMonth = YearMonth.from(periodCalculator.today());
        LocalDate start = currentMonth.atDay(1);
        LocalDate end = currentMonth.atEndOfMonth();

        BigDecimal income = incomeRepository.findSchedulesRelevantToRange(userId, start, end).stream()
                .map(entry -> entry.getAmount().multiply(BigDecimal.valueOf(countOccurrences(entry, start, end))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<DatedCategoryExpenseTotal> expenseTotals = expenseRepository
                .sumByDateAndCategoryAndUserIdBetween(userId, start, end);
        Map<String, BigDecimal> currentCategoryTotals = ExpensePeriodAggregation.byCategory(expenseTotals, start, end);
        BigDecimal expenses = currentCategoryTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyBudget = budgetRepository.findAllByUserIdOrderByPeriodAscCategoryAsc(userId).stream()
                .filter(budget -> "monthly".equalsIgnoreCase(budget.getPeriod()))
                .map(budget -> BigDecimal.valueOf(budget.getLimit()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var categories = currentCategoryTotals.entrySet().stream()
                .map(entry -> new MonthlySummary.CategorySpending(entry.getKey(), entry.getValue()))
                .toList();

        return new MonthlySummary(currentMonth, income, expenses, income.subtract(expenses), monthlyBudget, categories);
    }

    public PeriodSummary getPeriodSummary(Long userId, String period) {
        LocalDate today = periodCalculator.today();
        var windows = java.util.stream.IntStream.range(0, 6)
                .mapToObj(index -> periodCalculator.window(period, 5 - index, today))
                .toList();
        LocalDate start = windows.get(0).start();
        LocalDate end = windows.get(windows.size() - 1).end();
        var incomeEntries = incomeRepository.findSchedulesRelevantToRange(userId, start, end);
        List<DatedCategoryExpenseTotal> expenseTotals = expenseRepository
                .sumByDateAndCategoryAndUserIdBetween(userId, start, end);
        var trends = windows.stream()
                .map(window -> new PeriodTrend(window.start(), incomeTotal(incomeEntries, window.start(), window.end()),
                        ExpensePeriodAggregation.total(expenseTotals, window.start(), window.end())))
                .toList();
        FinancialPeriodCalculator.Window current = windows.get(windows.size() - 1);
        PeriodTrend currentTrend = trends.get(trends.size() - 1);
        PeriodTrend previousTrend = trends.get(trends.size() - 2);
        BigDecimal income = currentTrend.income();
        BigDecimal expenses = currentTrend.expenses();
        Map<String, BigDecimal> spendingByCategory = ExpensePeriodAggregation.byCategory(
                expenseTotals, current.start(), current.end());
        var categories = spendingByCategory.entrySet().stream()
                .map(entry -> new MonthlySummary.CategorySpending(entry.getKey(), entry.getValue()))
                .toList();
        var budgetUsage = budgetRepository.findAllByUserIdOrderByPeriodAscCategoryAsc(userId).stream()
                .filter(budget -> period.equalsIgnoreCase(budget.getPeriod()))
                .map(budget -> {
                    BigDecimal limit = BigDecimal.valueOf(budget.getLimit());
                    BigDecimal spent = spendingByCategory.getOrDefault(budget.getCategory(), BigDecimal.ZERO);
                    return new BudgetUsage(budget.getCategory(), limit, spent, limit.subtract(spent));
                })
                .toList();
        return new PeriodSummary(period, current.start(), current.end(), income, expenses, income.subtract(expenses),
                previousTrend.income(), previousTrend.expenses(), categories, budgetUsage, trends);
    }

    private BigDecimal incomeTotal(java.util.List<IncomeEntry> entries, LocalDate start, LocalDate end) {
        return entries.stream()
                .map(entry -> entry.getAmount().multiply(BigDecimal.valueOf(countOccurrences(entry, start, end))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isInMonth(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    private long countOccurrences(IncomeEntry entry, LocalDate start, LocalDate end) {
        LocalDate first = entry.getDate();
        String frequency = entry.getFrequency() == null ? "ONCE" : entry.getFrequency();
        if (frequency.equals("ONCE")) return isInMonth(first, start, end) ? 1 : 0;

        if (frequency.equals("WEEKLY") || frequency.equals("CUSTOM")) {
            int interval = frequency.equals("WEEKLY") ? 7 : Math.max(1, entry.getIntervalDays() == null ? 1 : entry.getIntervalDays());
            long daysToStart = Math.max(0, ChronoUnit.DAYS.between(first, start));
            long steps = (daysToStart + interval - 1) / interval;
            LocalDate occurrence = first.plusDays(steps * interval);
            long count = 0;
            while (!occurrence.isAfter(end)) {
                if (!occurrence.isBefore(start)) count++;
                occurrence = occurrence.plusDays(interval);
            }
            return count;
        }

        if (frequency.equals("MONTHLY")) {
            YearMonth month = YearMonth.from(first).isBefore(YearMonth.from(start)) ? YearMonth.from(start) : YearMonth.from(first);
            long count = 0;
            while (!month.atDay(1).isAfter(end)) {
                LocalDate occurrence = month.atDay(Math.min(first.getDayOfMonth(), month.lengthOfMonth()));
                if (!occurrence.isBefore(first) && !occurrence.isBefore(start) && !occurrence.isAfter(end)) count++;
                month = month.plusMonths(1);
            }
            return count;
        }

        if (frequency.equals("YEARLY")) {
            int firstYear = Math.max(first.getYear(), start.getYear());
            long count = 0;
            for (int year = firstYear; year <= end.getYear(); year++) {
                int day = Math.min(first.getDayOfMonth(), first.getMonth().length(java.time.Year.isLeap(year)));
                LocalDate occurrence = LocalDate.of(year, first.getMonth(), day);
                if (!occurrence.isBefore(first) && !occurrence.isBefore(start) && !occurrence.isAfter(end)) count++;
            }
            return count;
        }
        return isInMonth(first, start, end) ? 1 : 0;
    }
}
