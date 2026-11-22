package com.budgetbuddy.api.income;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "income_entries", indexes = {
        @Index(name = "idx_income_user_date", columnList = "user_id,date")
})
public class IncomeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 80)
    private String source;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 16, columnDefinition = "varchar(16) not null default 'ONCE'")
    private String frequency = "ONCE";

    @Column(name = "interval_days")
    private Integer intervalDays;

    protected IncomeEntry() {
        // JPA uses this constructor when it reads a row from MySQL.
    }

    public IncomeEntry(Long userId, String source, BigDecimal amount, LocalDate date, String frequency, Integer intervalDays) {
        this.userId = userId;
        updateDetails(source, amount, date, frequency, intervalDays);
    }

    public void updateDetails(String source, BigDecimal amount, LocalDate date, String frequency, Integer intervalDays) {
        this.source = source;
        this.amount = amount;
        this.date = date;
        this.frequency = frequency;
        this.intervalDays = intervalDays;
    }

    public Long getId() { return id; }
    public String getSource() { return source; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDate() { return date; }
    public String getFrequency() { return frequency; }
    public Integer getIntervalDays() { return intervalDays; }
}
