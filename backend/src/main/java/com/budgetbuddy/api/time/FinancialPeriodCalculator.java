package com.budgetbuddy.api.time;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

/** Supplies the application-local date and calendar windows used by finance summaries. */
public class FinancialPeriodCalculator {

    private final Clock clock;

    public FinancialPeriodCalculator(Clock clock) {
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public Window window(String period, int offset) {
        return window(period, offset, today());
    }

    public Window window(String period, int offset, LocalDate today) {
        String normalizedPeriod = period.toLowerCase(Locale.ROOT);
        LocalDate start = switch (normalizedPeriod) {
            case "weekly" -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset);
            case "yearly" -> LocalDate.of(today.getYear() - offset, 1, 1);
            default -> YearMonth.from(today).minusMonths(offset).atDay(1);
        };
        LocalDate end = switch (normalizedPeriod) {
            case "weekly" -> start.plusDays(6);
            case "yearly" -> LocalDate.of(start.getYear(), 12, 31);
            default -> YearMonth.from(start).atEndOfMonth();
        };
        return new Window(start, end);
    }

    public record Window(LocalDate start, LocalDate end) { }
}
