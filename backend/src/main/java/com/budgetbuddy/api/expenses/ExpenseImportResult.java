package com.budgetbuddy.api.expenses;

import java.util.List;

public record ExpenseImportResult(int importedCount, int duplicateCount, List<Expense> expenses) { }
