package com.budgetbuddy.api.expenses;

import java.util.List;

public record ExpensePageResponse(
        List<Expense> items,
        int page,
        int size,
        long totalElements,
        int totalPages) { }
