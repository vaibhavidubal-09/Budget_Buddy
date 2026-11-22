package com.budgetbuddy.api.summary;

import java.math.BigDecimal;

/** A user's category limit compared with categorized spending in the selected period. */
public record BudgetUsage(
        String category,
        BigDecimal limit,
        BigDecimal spent,
        BigDecimal remaining
) { }
