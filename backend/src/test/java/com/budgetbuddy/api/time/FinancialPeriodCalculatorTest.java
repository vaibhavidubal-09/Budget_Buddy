package com.budgetbuddy.api.time;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class FinancialPeriodCalculatorTest {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    @Test
    void weeklyWindowsAlwaysStartMondayAndEndSunday() {
        FinancialPeriodCalculator sundayCalculator = calculator("2026-10-18T06:30:00Z");
        FinancialPeriodCalculator mondayCalculator = calculator("2026-10-18T18:30:00Z");

        assertEquals(new FinancialPeriodCalculator.Window(LocalDate.parse("2026-10-12"),
                        LocalDate.parse("2026-10-18")), sundayCalculator.window("weekly", 0));
        assertEquals(new FinancialPeriodCalculator.Window(LocalDate.parse("2026-10-19"),
                        LocalDate.parse("2026-10-25")), mondayCalculator.window("weekly", 0));
    }

    @Test
    void periodWindowsRespectMonthAndYearBoundaries() {
        FinancialPeriodCalculator calculator = calculator("2025-12-31T18:30:00Z");

        assertEquals(new FinancialPeriodCalculator.Window(LocalDate.parse("2025-12-29"),
                        LocalDate.parse("2026-01-04")), calculator.window("weekly", 0));
        assertEquals(new FinancialPeriodCalculator.Window(LocalDate.parse("2026-01-01"),
                        LocalDate.parse("2026-01-31")), calculator.window("monthly", 0));
        assertEquals(new FinancialPeriodCalculator.Window(LocalDate.parse("2025-12-01"),
                        LocalDate.parse("2025-12-31")), calculator.window("monthly", 1));
        assertEquals(new FinancialPeriodCalculator.Window(LocalDate.parse("2026-01-01"),
                        LocalDate.parse("2026-12-31")), calculator.window("yearly", 0));
        assertEquals(new FinancialPeriodCalculator.Window(LocalDate.parse("2025-01-01"),
                        LocalDate.parse("2025-12-31")), calculator.window("yearly", 1));
    }

    @Test
    void localDateRollsOverAtMidnightInConfiguredTimezone() {
        FinancialPeriodCalculator beforeMidnight = calculator("2025-12-31T18:29:59Z");
        FinancialPeriodCalculator atMidnight = calculator("2025-12-31T18:30:00Z");

        assertEquals(LocalDate.parse("2025-12-31"), beforeMidnight.today());
        assertEquals(LocalDate.parse("2026-01-01"), atMidnight.today());
    }

    private FinancialPeriodCalculator calculator(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), INDIA);
        return new FinancialPeriodCalculator(clock);
    }
}
