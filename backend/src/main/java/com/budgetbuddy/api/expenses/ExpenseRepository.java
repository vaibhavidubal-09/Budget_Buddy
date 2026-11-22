package com.budgetbuddy.api.expenses;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.math.BigDecimal;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findAllByUserIdOrderByDateDesc(Long userId);
    List<Expense> findAllByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate start, LocalDate end);
    Optional<Expense> findByIdAndUserId(Long id, Long userId);
    Optional<Expense> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
    List<Expense> findAllByUserIdAndImportFingerprintIn(Long userId, List<String> fingerprints);

    @Query(value = "select expense from Expense expense where expense.userId = :userId " +
            "and (:category is null or expense.category = :category) " +
            "and (:search is null or lower(expense.description) like lower(concat('%', :search, '%'))) " +
            "and (:startDate is null or expense.date >= :startDate) " +
            "and (:endDate is null or expense.date <= :endDate)",
            countQuery = "select count(expense) from Expense expense where expense.userId = :userId " +
                    "and (:category is null or expense.category = :category) " +
                    "and (:search is null or lower(expense.description) like lower(concat('%', :search, '%'))) " +
                    "and (:startDate is null or expense.date >= :startDate) " +
                    "and (:endDate is null or expense.date <= :endDate)")
    Page<Expense> findPageByUser(@Param("userId") Long userId,
                                 @Param("category") String category,
                                 @Param("search") String search,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 Pageable pageable);

    @Query("select sum(expense.amount) from Expense expense " +
            "where expense.userId = :userId and expense.date between :start and :end")
    BigDecimal sumAmountByUserIdAndDateBetween(@Param("userId") Long userId,
                                                @Param("start") LocalDate start,
                                                @Param("end") LocalDate end);

    @Query("select expense.category as category, sum(expense.amount) as total from Expense expense " +
            "where expense.userId = :userId and expense.date between :start and :end " +
            "group by expense.category order by expense.category")
    List<CategoryExpenseTotal> sumByCategoryAndUserIdAndDateBetween(@Param("userId") Long userId,
                                                                     @Param("start") LocalDate start,
                                                                     @Param("end") LocalDate end);

    @Query("select new com.budgetbuddy.api.expenses.DatedCategoryExpenseTotal(" +
            "expense.date, expense.category, sum(expense.amount)) from Expense expense " +
            "where expense.userId = :userId and expense.date between :start and :end " +
            "group by expense.date, expense.category order by expense.date, expense.category")
    List<DatedCategoryExpenseTotal> sumByDateAndCategoryAndUserIdBetween(@Param("userId") Long userId,
                                                                          @Param("start") LocalDate start,
                                                                          @Param("end") LocalDate end);
}
