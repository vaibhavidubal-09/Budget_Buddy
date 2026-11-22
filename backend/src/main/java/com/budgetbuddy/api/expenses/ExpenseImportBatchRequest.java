package com.budgetbuddy.api.expenses;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record ExpenseImportBatchRequest(
        @NotEmpty @Size(max = 1000) List<@Valid ExpenseImportRequest> transactions) { }
