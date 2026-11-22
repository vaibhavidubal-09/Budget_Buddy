package com.budgetbuddy.api.income;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class IncomeServiceTest {

    @Mock
    private IncomeRepository incomeRepository;

    @InjectMocks
    private IncomeService incomeService;

    @Test
    void customScheduleWithoutIntervalIsRejectedBeforeSaving() {
        IncomeRequest request = new IncomeRequest();
        request.setSource("Allowance");
        request.setAmount(new BigDecimal("2000.00"));
        request.setDate(LocalDate.parse("2026-10-12"));
        request.setFrequency("CUSTOM");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> incomeService.addIncome(request, 42L));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(incomeRepository, never()).save(any(IncomeEntry.class));
    }

    @Test
    void customScheduleSavesItsInterval() {
        IncomeRequest request = new IncomeRequest();
        request.setSource("Allowance");
        request.setAmount(new BigDecimal("2000.00"));
        request.setDate(LocalDate.parse("2026-10-12"));
        request.setFrequency("CUSTOM");
        request.setIntervalDays(14);
        when(incomeRepository.save(any(IncomeEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncomeEntry saved = incomeService.addIncome(request, 42L);

        assertEquals("CUSTOM", saved.getFrequency());
        assertEquals(14, saved.getIntervalDays());
        verify(incomeRepository).save(any(IncomeEntry.class));
    }
}
