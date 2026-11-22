package com.budgetbuddy.api.income;

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
@RequestMapping("/api/income")
public class IncomeController {

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @GetMapping
    public List<IncomeEntry> getIncome(@AuthenticationPrincipal Jwt jwt) {
        return incomeService.getAllIncome(JwtUserId.from(jwt));
    }

    @GetMapping("/{id}")
    public IncomeEntry getIncome(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return incomeService.getIncome(id, JwtUserId.from(jwt));
    }

    @PostMapping
    public ResponseEntity<IncomeEntry> addIncome(@Valid @RequestBody IncomeRequest request, @AuthenticationPrincipal Jwt jwt) {
        IncomeEntry savedIncome = incomeService.addIncome(request, JwtUserId.from(jwt));
        return ResponseEntity.created(URI.create("/api/income/" + savedIncome.getId())).body(savedIncome);
    }

    @PutMapping("/{id}")
    public IncomeEntry updateIncome(@PathVariable Long id, @Valid @RequestBody IncomeRequest request, @AuthenticationPrincipal Jwt jwt) {
        return incomeService.updateIncome(id, request, JwtUserId.from(jwt));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncome(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        incomeService.deleteIncome(id, JwtUserId.from(jwt));
        return ResponseEntity.noContent().build();
    }
}
