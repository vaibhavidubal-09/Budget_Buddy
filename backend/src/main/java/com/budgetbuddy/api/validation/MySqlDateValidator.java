package com.budgetbuddy.api.validation;

import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MySqlDateValidator implements ConstraintValidator<MySqlDate, LocalDate> {
    private static final LocalDate MINIMUM = LocalDate.of(1000, 1, 1);
    private static final LocalDate MAXIMUM = LocalDate.of(9999, 12, 31);

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        return value == null || (!value.isBefore(MINIMUM) && !value.isAfter(MAXIMUM));
    }
}
