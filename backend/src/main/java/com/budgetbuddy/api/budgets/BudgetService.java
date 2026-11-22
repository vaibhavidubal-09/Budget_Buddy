package com.budgetbuddy.api.budgets;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.budgetbuddy.api.auth.UserAccountRepository;

@Service
@Transactional
public class BudgetService {

    private static final String DUPLICATE_BUDGET_MESSAGE =
            "A budget already exists for this category and period.";
    private static final String BUDGET_UNIQUE_INDEX = "uk_category_budgets_user_period_category";

    private final BudgetRepository budgetRepository;
    private final UserAccountRepository userAccountRepository;

    public BudgetService(BudgetRepository budgetRepository, UserAccountRepository userAccountRepository) {
        this.budgetRepository = budgetRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<Budget> getAllBudgets(Long userId) {
        return budgetRepository.findAllByUserIdOrderByPeriodAscCategoryAsc(userId);
    }

    @Transactional(readOnly = true)
    public Budget getBudget(Long id, Long userId) {
        return findBudget(id, userId);
    }

    public Budget addBudget(BudgetRequest request, Long userId) {
        lockOwner(userId);
        Budget budget = new Budget(userId, request.getCategory(), request.getLimit(), request.getPeriod());
        rejectDuplicate(userId, budget.getCategory(), budget.getPeriod(), null);
        try {
            return budgetRepository.saveAndFlush(budget);
        } catch (DataIntegrityViolationException exception) {
            if (isBudgetUniqueViolation(exception)) {
                throw duplicateBudget(exception);
            }
            throw exception;
        }
    }

    public Budget updateBudget(Long id, BudgetRequest request, Long userId) {
        lockOwner(userId);
        Budget budget = findBudget(id, userId);
        if (request.getVersion() == null || !request.getVersion().equals(budget.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This budget changed since you loaded it. Refresh the page and try again.");
        }
        budget.updateDetails(request.getCategory(), request.getLimit(), request.getPeriod());
        rejectDuplicate(userId, budget.getCategory(), budget.getPeriod(), id);
        try {
            return budgetRepository.saveAndFlush(budget);
        } catch (DataIntegrityViolationException exception) {
            if (isBudgetUniqueViolation(exception)) {
                throw duplicateBudget(exception);
            }
            throw exception;
        }
    }

    public void deleteBudget(Long id, Long userId) {
        lockOwner(userId);
        budgetRepository.delete(findBudget(id, userId));
    }

    /** Replaces this user's complete budget set for one period as a single atomic operation. */
    public List<Budget> applyPlan(BudgetPlanApplyRequest request, Long userId) {
        validatePlan(request);
        lockOwner(userId);

        List<Budget> existing = budgetRepository.findAllByUserIdAndPeriodOrderByCategoryAsc(userId, request.period());
        Map<Long, Integer> currentVersions = new HashMap<>();
        for (Budget budget : existing) currentVersions.put(budget.getId(), budget.getVersion());

        Map<Long, Integer> expectedVersions = new HashMap<>();
        for (ExpectedBudgetVersion expected : request.expectedBudgets()) {
            if (expectedVersions.putIfAbsent(expected.id(), expected.version()) != null) {
                throw badRequest("The expected budget list contains a repeated budget id.");
            }
        }
        if (!currentVersions.equals(expectedVersions)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Budgets for this period changed since you opened the plan. Reload and try again.");
        }

        Map<String, Budget> existingByCategory = new HashMap<>();
        for (Budget budget : existing) {
            String key = categoryKey(budget.getCategory());
            if (existingByCategory.putIfAbsent(key, budget) != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This period already contains duplicate category budgets. Resolve them before applying a plan.");
            }
        }

        Set<String> requestedCategories = new HashSet<>();
        List<Budget> toSave = new ArrayList<>();
        for (BudgetPlanItemRequest item : request.budgets()) {
            String category = item.category().strip();
            if (!BudgetCategories.ALLOWED.contains(category)) {
                throw badRequest("Choose a category from the supported budget categories.");
            }
            String key = categoryKey(category);
            if (!requestedCategories.add(key)) {
                throw badRequest("A category can appear only once in a budget plan.");
            }
            Budget budget = existingByCategory.remove(key);
            if (budget == null) budget = new Budget(userId, category, item.limit(), request.period());
            else budget.updateDetails(category, item.limit(), request.period());
            toSave.add(budget);
        }

        try {
            // Deletes are versioned entity deletes; all changes flush in this same transaction.
            budgetRepository.deleteAll(existingByCategory.values());
            budgetRepository.saveAllAndFlush(toSave);
            return budgetRepository.findAllByUserIdOrderByPeriodAscCategoryAsc(userId);
        } catch (DataIntegrityViolationException exception) {
            if (isBudgetUniqueViolation(exception)) throw duplicateBudget(exception);
            throw exception;
        }
    }

    private Budget findBudget(Long id, Long userId) {
        return budgetRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Budget " + id + " was not found"));
    }

    private void rejectDuplicate(Long userId, String category, String period, Long excludedBudgetId) {
        boolean exists = excludedBudgetId == null
                ? budgetRepository.existsByUserIdAndPeriodAndCategoryIgnoreCase(userId, period, category)
                : budgetRepository.existsByUserIdAndPeriodAndCategoryIgnoreCaseAndIdNot(
                        userId, period, category, excludedBudgetId);
        if (exists) {
            throw duplicateBudget(null);
        }
    }

    private ResponseStatusException duplicateBudget(Throwable cause) {
        return new ResponseStatusException(HttpStatus.CONFLICT, DUPLICATE_BUDGET_MESSAGE, cause);
    }

    private boolean isBudgetUniqueViolation(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && BUDGET_UNIQUE_INDEX.equalsIgnoreCase(violation.getConstraintName())) {
                return true;
            }
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(java.util.Locale.ROOT)
                    .contains(BUDGET_UNIQUE_INDEX)) {
                return true;
            }
        }
        return false;
    }

    private void validatePlan(BudgetPlanApplyRequest request) {
        if (request == null || !List.of("weekly", "monthly", "yearly").contains(request.period())) {
            throw badRequest("Choose a weekly, monthly, or yearly budget period.");
        }
        if (request.budgets() == null || request.expectedBudgets() == null) {
            throw badRequest("A budget plan must include budgets and the expected budget versions.");
        }
        Set<String> categories = new HashSet<>();
        for (BudgetPlanItemRequest item : request.budgets()) {
            if (item == null || item.category() == null || item.category().isBlank()
                    || item.category().strip().length() > 60 || item.limit() == null || item.limit() <= 0) {
                throw badRequest("Each budget needs a supported category and a positive whole-number limit.");
            }
            String category = item.category().strip();
            if (!BudgetCategories.ALLOWED.contains(category)) {
                throw badRequest("Choose a category from the supported budget categories.");
            }
            if (!categories.add(categoryKey(category))) {
                throw badRequest("A category can appear only once in a budget plan.");
            }
        }
        Set<Long> expectedIds = new HashSet<>();
        for (ExpectedBudgetVersion expected : request.expectedBudgets()) {
            if (expected == null || expected.id() == null || expected.id() <= 0
                    || expected.version() == null || expected.version() < 0) {
                throw badRequest("The expected budget versions are invalid.");
            }
            if (!expectedIds.add(expected.id())) {
                throw badRequest("The expected budget list contains a repeated budget id.");
            }
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private String categoryKey(String category) {
        return category.strip().toLowerCase(Locale.ROOT);
    }

    private void lockOwner(Long userId) {
        userAccountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found"));
    }
}
