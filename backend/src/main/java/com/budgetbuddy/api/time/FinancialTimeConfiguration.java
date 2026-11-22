package com.budgetbuddy.api.time;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FinancialTimeConfiguration {

    @Bean
    public Clock applicationClock(@Value("${budgetbuddy.time-zone:Asia/Kolkata}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }

    @Bean
    public FinancialPeriodCalculator financialPeriodCalculator(Clock applicationClock) {
        return new FinancialPeriodCalculator(applicationClock);
    }
}
