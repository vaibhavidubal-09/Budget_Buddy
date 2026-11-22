package com.budgetbuddy.api.budgets;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "category_budgets", indexes = {
        @Index(name = "idx_budgets_user_period_category", columnList = "user_id,period,category")
})
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(name = "budget_limit", nullable = false)
    private Integer limit;

    @Column(nullable = false, length = 12)
    private String period;

    @Version
    @Column(nullable = false)
    private Integer version;

    protected Budget() {
        // JPA uses this constructor when it reads a row from MySQL.
    }

    public Budget(Long userId, String category, Integer limit, String period) {
        this.userId = userId;
        updateDetails(category, limit, period);
    }

    public void updateDetails(String category, Integer limit, String period) {
        this.category = category == null ? null : category.strip();
        this.limit = limit;
        this.period = period;
    }

    public Long getId() { return id; }
    public String getCategory() { return category; }
    public Integer getLimit() { return limit; }
    public String getPeriod() { return period; }
    public Integer getVersion() { return version; }
}
