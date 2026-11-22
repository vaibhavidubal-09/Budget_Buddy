package com.budgetbuddy.api.expenses;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record BudgetCategorySuggestionRequest(
        @NotBlank @Pattern(regexp = "weekly|monthly|yearly", message = "Period must be weekly, monthly, or yearly")
        String period,
        @NotNull @Size(max = 32)
        Map<@NotBlank @Size(max = 60) String,
                @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal> spendingByCategory,
        @NotNull @Size(max = 32) List<@NotBlank @Size(max = 60) String> availableCategories) { }
