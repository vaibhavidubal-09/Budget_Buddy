package com.budgetbuddy.api.budgets;

import java.util.List;

public final class BudgetCategories {

    public static final List<String> ALLOWED = List.of(
            "Food & Dining", "Transportation", "Shopping", "Education",
            "Entertainment", "Bills & Subscriptions", "Healthcare", "Other");

    private BudgetCategories() { }
}
