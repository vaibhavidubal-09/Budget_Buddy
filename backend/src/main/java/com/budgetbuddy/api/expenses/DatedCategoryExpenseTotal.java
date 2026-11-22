package com.budgetbuddy.api.expenses;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Database aggregate for one date and expense category. */
public record DatedCategoryExpenseTotal(LocalDate date, String category, BigDecimal total) { }
