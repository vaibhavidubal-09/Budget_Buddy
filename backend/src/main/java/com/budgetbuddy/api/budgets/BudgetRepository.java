package com.budgetbuddy.api.budgets;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findAllByUserIdOrderByPeriodAscCategoryAsc(Long userId);
    List<Budget> findAllByUserIdAndPeriodOrderByCategoryAsc(Long userId, String period);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndPeriodAndCategoryIgnoreCase(Long userId, String period, String category);
    boolean existsByUserIdAndPeriodAndCategoryIgnoreCaseAndIdNot(
            Long userId, String period, String category, Long id);
}
