package com.budgetbuddy.api.budgets;

import java.math.BigDecimal;

public record BudgetPlanAllocation(String category, BigDecimal limit) { }
