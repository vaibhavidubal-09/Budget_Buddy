package com.budgetbuddy.api.expenses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategorySuggestionRequest {

    @NotBlank
    @Size(max = 120)
    private String description;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
