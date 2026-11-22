package com.budgetbuddy.api.summary;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record MonthlySummary(
        YearMonth month,
        BigDecimal income,
        BigDecimal expenses,
        BigDecimal balance,
        BigDecimal monthlyBudget,
        List<CategorySpending> categorySpending
) {
    public record CategorySpending(String category, BigDecimal amount) { }
}
