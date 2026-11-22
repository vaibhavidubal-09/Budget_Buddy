package com.budgetbuddy.api.expenses;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ExpenseUnderstandingRequest {

    @NotBlank
    @Size(max = 500)
    private String description;

    @NotNull
    private LocalDate today;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getToday() { return today; }
    public void setToday(LocalDate today) { this.today = today; }
}
