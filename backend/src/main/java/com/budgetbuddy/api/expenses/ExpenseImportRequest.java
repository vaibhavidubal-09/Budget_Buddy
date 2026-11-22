package com.budgetbuddy.api.expenses;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import com.budgetbuddy.api.validation.MySqlDate;

public record ExpenseImportRequest(
        @NotBlank @Size(max = 80) String rowId,
        @NotBlank @Size(max = 120) String description,
        @NotBlank @Size(max = 60) String category,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 30) String paymentMethod,
        @NotNull @MySqlDate LocalDate date,
        @Size(max = 120) String sourceTransactionId) { }
