package com.budgetbuddy.api.expenses;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseUnderstandingResponse(
        BigDecimal amount,
        String description,
        String category,
        String paymentMethod,
        LocalDate date,
        String spellingSuggestion) { }
