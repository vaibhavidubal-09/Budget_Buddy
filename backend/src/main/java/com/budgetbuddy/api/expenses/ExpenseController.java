package com.budgetbuddy.api.expenses;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import com.budgetbuddy.api.auth.JwtUserId;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public List<Expense> getExpenses(@AuthenticationPrincipal Jwt jwt) {
        return expenseService.getAllExpenses(JwtUserId.from(jwt));
    }

    @GetMapping("/page")
    public ExpensePageResponse getExpensePage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @AuthenticationPrincipal Jwt jwt) {
        return expenseService.getExpensePage(JwtUserId.from(jwt), page, size, category, search, startDate, endDate);
    }

    @GetMapping("/{id}")
    public Expense getExpense(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return expenseService.getExpense(id, JwtUserId.from(jwt));
    }

    @PostMapping
    public ResponseEntity<Expense> addExpense(@Valid @RequestBody ExpenseRequest request,
                                               @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                               @AuthenticationPrincipal Jwt jwt) {
        if (idempotencyKey != null && (idempotencyKey.isBlank() || idempotencyKey.length() > 128)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must contain 1 to 128 characters");
        }
        ExpenseCreateResult result = expenseService.addExpense(request, JwtUserId.from(jwt), idempotencyKey);
        if (result.replayed()) return ResponseEntity.ok(result.expense());
        return ResponseEntity.created(URI.create("/api/expenses/" + result.expense().getId())).body(result.expense());
    }

    @PostMapping("/import/preview")
    public ExpenseImportPreviewResponse previewImport(@Valid @RequestBody ExpenseImportBatchRequest request,
                                                       @AuthenticationPrincipal Jwt jwt) {
        return expenseService.previewImport(request, JwtUserId.from(jwt));
    }

    @PostMapping("/import")
    public ExpenseImportResult importExpenses(@Valid @RequestBody ExpenseImportBatchRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        return expenseService.importBatch(request, JwtUserId.from(jwt));
    }

    @PutMapping("/{id}")
    public Expense updateExpense(@PathVariable Long id, @Valid @RequestBody ExpenseRequest request, @AuthenticationPrincipal Jwt jwt) {
        return expenseService.updateExpense(id, request, JwtUserId.from(jwt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        expenseService.deleteExpense(id, JwtUserId.from(jwt));
        return ResponseEntity.noContent().build();
    }
}
