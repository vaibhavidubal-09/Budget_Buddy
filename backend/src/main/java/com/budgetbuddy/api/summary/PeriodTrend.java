package com.budgetbuddy.api.summary;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Totals for one bucket in the six-period analytics chart. */
public record PeriodTrend(LocalDate startDate, BigDecimal income, BigDecimal expenses) { }
