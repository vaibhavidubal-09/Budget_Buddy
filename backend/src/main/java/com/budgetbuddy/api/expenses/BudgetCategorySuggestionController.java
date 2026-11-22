package com.budgetbuddy.api.expenses;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
public class BudgetCategorySuggestionController {

    private final CategorySuggestionService categorySuggestionService;

    public BudgetCategorySuggestionController(CategorySuggestionService categorySuggestionService) {
        this.categorySuggestionService = categorySuggestionService;
    }

    @PostMapping("/suggest-category")
    public CategorySuggestion suggestCategory(@Valid @RequestBody BudgetCategorySuggestionRequest request) {
        return categorySuggestionService.suggestBudgetCategory(request.period(), request.spendingByCategory(),
                request.availableCategories());
    }
}
