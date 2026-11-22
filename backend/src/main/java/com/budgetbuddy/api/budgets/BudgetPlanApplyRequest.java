package com.budgetbuddy.api.budgets;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record BudgetPlanApplyRequest(
        @NotBlank @Pattern(regexp = "weekly|monthly|yearly", message = "Period must be weekly, monthly, or yearly")
        String period,
        @NotNull List<@Valid BudgetPlanItemRequest> budgets,
        @NotNull List<@Valid ExpectedBudgetVersion> expectedBudgets) { }
