package com.budgetbuddy.api.expenses;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseUnderstandingController {

    private final ExpenseUnderstandingService expenseUnderstandingService;

    public ExpenseUnderstandingController(ExpenseUnderstandingService expenseUnderstandingService) {
        this.expenseUnderstandingService = expenseUnderstandingService;
    }

    @PostMapping("/understand")
    public ExpenseUnderstandingResponse understand(@Valid @RequestBody ExpenseUnderstandingRequest request) {
        return expenseUnderstandingService.understand(request.getDescription(), request.getToday());
    }
}
