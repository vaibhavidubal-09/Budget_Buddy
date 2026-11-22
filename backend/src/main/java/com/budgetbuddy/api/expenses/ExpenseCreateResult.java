package com.budgetbuddy.api.expenses;

public record ExpenseCreateResult(Expense expense, boolean replayed) {
}
