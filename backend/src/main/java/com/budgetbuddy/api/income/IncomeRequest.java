package com.budgetbuddy.api.income;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Digits;
import com.budgetbuddy.api.validation.MySqlDate;

public class IncomeRequest {

    @NotBlank
    @Size(max = 80)
    private String source;

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    @NotNull
    @MySqlDate
    private LocalDate date;

    @Pattern(regexp = "ONCE|WEEKLY|MONTHLY|YEARLY|CUSTOM")
    private String frequency = "ONCE";

    @Min(1)
    @Max(3650)
    private Integer intervalDays;

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getFrequency() { return frequency == null ? "ONCE" : frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public Integer getIntervalDays() { return intervalDays; }
    public void setIntervalDays(Integer intervalDays) { this.intervalDays = intervalDays; }
}
