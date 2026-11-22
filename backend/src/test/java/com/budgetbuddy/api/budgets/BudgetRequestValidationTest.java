package com.budgetbuddy.api.budgets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.budgetbuddy.api.expenses.BudgetCategorySuggestionRequest;
import com.budgetbuddy.api.expenses.ExpenseRequest;
import com.budgetbuddy.api.auth.LoginRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BudgetRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void validBudgetRequestPassesValidation() {
        BudgetRequest request = new BudgetRequest();
        request.setCategory("Food & Dining");
        request.setLimit(5000);
        request.setPeriod("monthly");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void nonPositiveLimitAndUnknownPeriodFailValidation() {
        BudgetRequest request = new BudgetRequest();
        request.setCategory("Food & Dining");
        request.setLimit(0);
        request.setPeriod("daily");

        Set<?> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.size() >= 2);
    }

    @Test
    void validAiSuggestionRequestsPassValidation() {
        BudgetPlanRequest plan = new BudgetPlanRequest("monthly", new BigDecimal("5000.00"),
                new BigDecimal("1000.00"), "Emergency fund", List.of("Food & Dining", "Healthcare"));
        BudgetCategorySuggestionRequest category = new BudgetCategorySuggestionRequest("monthly",
                Map.of("Food & Dining", new BigDecimal("250.00")), List.of("Food & Dining"));

        assertTrue(validator.validate(plan).isEmpty());
        assertTrue(validator.validate(category).isEmpty());
    }

    @Test
    void invalidAiSuggestionRequestsFailValidation() {
        BudgetPlanRequest plan = new BudgetPlanRequest("daily", new BigDecimal("12.345"),
                new BigDecimal("13.00"), " ", List.of());
        BudgetCategorySuggestionRequest category = new BudgetCategorySuggestionRequest("daily",
                Map.of("Food & Dining", new BigDecimal("-1.00")), null);

        assertFalse(validator.validate(plan).isEmpty());
        assertFalse(validator.validate(category).isEmpty());
    }

    @Test
    void loginAndExpenseOptionalDetailsHaveInputBounds() {
        LoginRequest login = new LoginRequest();
        login.setEmail("a".repeat(255) + "@example.com");
        login.setPassword("x".repeat(73));

        ExpenseRequest expense = new ExpenseRequest();
        expense.setDescription("Lunch");
        expense.setCategory("Food & Dining");
        expense.setAmount(new BigDecimal("10.00"));
        expense.setPaymentMethod("Cash");
        expense.setDate(java.time.LocalDate.of(2026, 10, 18));
        expense.setSplitDetails("x".repeat(16001));

        assertFalse(validator.validate(login).isEmpty());
        assertFalse(validator.validate(expense).isEmpty());
    }

    @Test
    void persistedDateFieldsRejectDatesOutsideMysqlDateRange() {
        assertTrue(validator.validateValue(ExpenseRequest.class, "date",
                java.time.LocalDate.of(1000, 1, 1)).isEmpty());
        assertTrue(validator.validateValue(ExpenseRequest.class, "date",
                java.time.LocalDate.of(9999, 12, 31)).isEmpty());
        assertFalse(validator.validateValue(ExpenseRequest.class, "date",
                java.time.LocalDate.of(999, 12, 31)).isEmpty());
        assertFalse(validator.validateValue(ExpenseRequest.class, "date",
                java.time.LocalDate.of(10000, 1, 1)).isEmpty());
    }

    @Test
    void passwordValidationUsesTheUtf8ByteLimitExpectedByBcrypt() {
        String multibytePassword = "Aa" + "é".repeat(68) + "1!";

        assertEquals(72, multibytePassword.length());
        assertFalse(validator.validateValue(LoginRequest.class, "password", multibytePassword).isEmpty());
        assertTrue(validator.validateValue(LoginRequest.class, "password", "StrongPass9!").isEmpty());
    }
}
