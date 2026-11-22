package com.budgetbuddy.api.budgets;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record ExpectedBudgetVersion(
        @NotNull @Positive Long id,
        @NotNull @PositiveOrZero Integer version) { }
