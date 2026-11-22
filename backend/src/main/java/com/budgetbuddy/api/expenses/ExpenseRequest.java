package com.budgetbuddy.api.expenses;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import com.budgetbuddy.api.validation.MySqlDate;

public class ExpenseRequest {

    @NotBlank
    @Size(max = 120)
    private String description;

    @NotBlank
    @Size(max = 60)
    private String category;

    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 2)
    private BigDecimal amount;

    @NotBlank
    @Size(max = 30)
    private String paymentMethod;

    @NotNull
    @MySqlDate
    private LocalDate date;

    @Size(max = 16000)
    private String splitDetails;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getSplitDetails() { return splitDetails; }
    public void setSplitDetails(String splitDetails) { this.splitDetails = splitDetails; }
}
