package com.budgetbuddy.api.budgets;

import java.util.List;

public record BudgetPlanResponse(String explanation, List<BudgetPlanAllocation> allocations) { }
