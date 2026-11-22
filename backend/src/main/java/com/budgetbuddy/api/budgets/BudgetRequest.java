package com.budgetbuddy.api.budgets;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class BudgetRequest {

    @NotBlank
    @Size(max = 60)
    private String category;

    @NotNull
    @Positive
    private Integer limit;

    @NotBlank
    @Pattern(regexp = "weekly|monthly|yearly", message = "Period must be weekly, monthly, or yearly")
    private String period;

    @PositiveOrZero
    private Integer version;

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
