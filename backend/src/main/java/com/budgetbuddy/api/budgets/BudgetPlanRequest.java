package com.budgetbuddy.api.budgets;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record BudgetPlanRequest(
        @NotBlank @Pattern(regexp = "weekly|monthly|yearly", message = "Period must be weekly, monthly, or yearly")
        String period,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal income,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal savingsGoal,
        @NotBlank @Size(max = 100) String savingsPurpose,
        @NotEmpty @Size(max = 32) List<@NotBlank @Size(max = 60) String> categories) { }
