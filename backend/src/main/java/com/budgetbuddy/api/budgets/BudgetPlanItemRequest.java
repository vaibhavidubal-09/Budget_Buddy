package com.budgetbuddy.api.budgets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record BudgetPlanItemRequest(
        @NotBlank @Size(max = 60) String category,
        @NotNull @Positive Integer limit) { }
