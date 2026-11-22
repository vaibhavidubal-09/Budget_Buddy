package com.budgetbuddy.api.summary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.budgetbuddy.api.expenses.DatedCategoryExpenseTotal;

/** Reduces the database's date/category aggregates to one of the already-calculated financial windows. */
public final class ExpensePeriodAggregation {

    private ExpensePeriodAggregation() { }

    public static Map<String, BigDecimal> byCategory(List<DatedCategoryExpenseTotal> totals,
                                                      LocalDate start, LocalDate end) {
        Map<String, BigDecimal> result = new TreeMap<>();
        for (DatedCategoryExpenseTotal total : totals) {
            if (!total.date().isBefore(start) && !total.date().isAfter(end)) {
                result.merge(total.category(), total.total(), BigDecimal::add);
            }
        }
        return result;
    }

    public static BigDecimal total(List<DatedCategoryExpenseTotal> totals, LocalDate start, LocalDate end) {
        return byCategory(totals, start, end).values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
