package com.budgetbuddy.api.expenses;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
public class CategorySuggestionController {

    private final CategorySuggestionService categorySuggestionService;

    public CategorySuggestionController(CategorySuggestionService categorySuggestionService) {
        this.categorySuggestionService = categorySuggestionService;
    }

    @PostMapping("/suggest-category")
    public CategorySuggestion suggestCategory(@Valid @RequestBody CategorySuggestionRequest request) {
        return categorySuggestionService.suggest(request.getDescription());
    }
}
