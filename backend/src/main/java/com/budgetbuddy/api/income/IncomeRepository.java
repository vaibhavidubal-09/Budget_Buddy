package com.budgetbuddy.api.income;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncomeRepository extends JpaRepository<IncomeEntry, Long> {
    List<IncomeEntry> findAllByUserIdOrderByDateDesc(Long userId);
    Optional<IncomeEntry> findByIdAndUserId(Long id, Long userId);

    /**
     * Returns one-time income dated in the requested range and recurring schedules started by its end.
     * Earlier recurring entries remain relevant because their schedule continues indefinitely.
     */
    @Query("select income from IncomeEntry income where income.userId = :userId " +
            "and income.date <= :end and (income.frequency is null or income.frequency <> 'ONCE' " +
            "or income.date >= :start) order by income.date desc")
    List<IncomeEntry> findSchedulesRelevantToRange(@Param("userId") Long userId,
                                                    @Param("start") LocalDate start,
                                                    @Param("end") LocalDate end);
}
