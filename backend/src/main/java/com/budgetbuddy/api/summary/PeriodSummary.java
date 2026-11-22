package com.budgetbuddy.api.summary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Server-calculated totals for a period and the immediately preceding equal-length period. */
public record PeriodSummary(
        String period,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal balance,
        BigDecimal previousIncome,
        BigDecimal previousExpenses,
        List<MonthlySummary.CategorySpending> categorySpending,
        List<BudgetUsage> budgetUsage,
        List<PeriodTrend> trends
) { }
