package com.budgetbuddy.api.expenses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.budgetbuddy.api.auth.UserAccountRepository;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private ExpenseService expenseService;

    @Test
    void addExpenseSavesTheSplitDetailsForTheOwner() {
        ExpenseRequest request = new ExpenseRequest();
        request.setDescription("Shared lunch");
        request.setCategory("Food & Dining");
        request.setAmount(new BigDecimal("300.00"));
        request.setPaymentMethod("UPI");
        request.setDate(LocalDate.parse("2026-10-12"));
        request.setSplitDetails("{\"mode\":\"equal\",\"payerIndex\":0}");
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Expense saved = expenseService.addExpense(request, 42L);

        assertEquals("Shared lunch", saved.getDescription());
        assertEquals(new BigDecimal("300.00"), saved.getAmount());
        assertEquals(request.getSplitDetails(), saved.getSplitDetails());
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    void getExpenseReturnsNotFoundWhenRecordDoesNotBelongToUser() {
        when(expenseRepository.findByIdAndUserId(7L, 42L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> expenseService.getExpense(7L, 42L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(expenseRepository).findByIdAndUserId(7L, 42L);
    }
}
