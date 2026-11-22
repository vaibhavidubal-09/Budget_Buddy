package com.budgetbuddy.api.budgets;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import com.budgetbuddy.api.auth.JwtUserId;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final BudgetPlanSuggestionService budgetPlanSuggestionService;

    public BudgetController(BudgetService budgetService, BudgetPlanSuggestionService budgetPlanSuggestionService) {
        this.budgetService = budgetService;
        this.budgetPlanSuggestionService = budgetPlanSuggestionService;
    }

    @GetMapping
    public List<Budget> getBudgets(@AuthenticationPrincipal Jwt jwt) {
        return budgetService.getAllBudgets(JwtUserId.from(jwt));
    }

    @PostMapping("/suggest-plan")
    public BudgetPlanResponse suggestPlan(@Valid @RequestBody BudgetPlanRequest request) {
        return budgetPlanSuggestionService.suggest(request);
    }

    @GetMapping("/{id}")
    public Budget getBudget(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return budgetService.getBudget(id, JwtUserId.from(jwt));
    }

    @PostMapping
    public ResponseEntity<Budget> addBudget(@Valid @RequestBody BudgetRequest request, @AuthenticationPrincipal Jwt jwt) {
        Budget savedBudget = budgetService.addBudget(request, JwtUserId.from(jwt));
        return ResponseEntity.created(URI.create("/api/budgets/" + savedBudget.getId())).body(savedBudget);
    }

    @PutMapping("/{id}")
    public Budget updateBudget(@PathVariable Long id, @Valid @RequestBody BudgetRequest request, @AuthenticationPrincipal Jwt jwt) {
        return budgetService.updateBudget(id, request, JwtUserId.from(jwt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        budgetService.deleteBudget(id, JwtUserId.from(jwt));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/apply-plan")
    public List<Budget> applyPlan(@Valid @RequestBody BudgetPlanApplyRequest request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return budgetService.applyPlan(request, JwtUserId.from(jwt));
    }
}
