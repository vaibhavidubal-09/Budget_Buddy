package com.budgetbuddy.api.expenses;

import java.math.BigDecimal;

/** A database projection containing the total spent in one category. */
public interface CategoryExpenseTotal {
    String getCategory();
    BigDecimal getTotal();
}
