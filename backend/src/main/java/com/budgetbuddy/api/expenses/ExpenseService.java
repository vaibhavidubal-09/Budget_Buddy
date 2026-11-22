package com.budgetbuddy.api.expenses;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;
import java.math.RoundingMode;
import com.budgetbuddy.api.auth.UserAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserAccountRepository userAccountRepository;

    public ExpenseService(ExpenseRepository expenseRepository, UserAccountRepository userAccountRepository) {
        this.expenseRepository = expenseRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public ExpenseImportPreviewResponse previewImport(ExpenseImportBatchRequest request, Long userId) {
        List<String> fingerprints = request.transactions().stream().map(ExpenseService::fingerprint).toList();
        Set<String> existing = new HashSet<>();
        if (!fingerprints.isEmpty()) {
            expenseRepository.findAllByUserIdAndImportFingerprintIn(userId, fingerprints)
                    .forEach(expense -> existing.add(readFingerprint(expense)));
        }
        Set<String> seen = new HashSet<>();
        List<ExpenseImportPreviewResponse.Row> rows = new ArrayList<>();
        int duplicates = 0;
        for (int index = 0; index < fingerprints.size(); index++) {
            String key = fingerprints.get(index);
            boolean duplicate = existing.contains(key) || !seen.add(key);
            if (duplicate) duplicates++;
            rows.add(new ExpenseImportPreviewResponse.Row(request.transactions().get(index).rowId(), duplicate,
                    existing.contains(key) ? "Already imported" : duplicate ? "Repeated in this file" : null));
        }
        return new ExpenseImportPreviewResponse(rows, duplicates);
    }

    public ExpenseImportResult importBatch(ExpenseImportBatchRequest request, Long userId) {
        userAccountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found"));
        List<String> fingerprints = request.transactions().stream().map(ExpenseService::fingerprint).toList();
        Set<String> existing = new HashSet<>();
        expenseRepository.findAllByUserIdAndImportFingerprintIn(userId, fingerprints)
                .forEach(expense -> existing.add(readFingerprint(expense)));
        Set<String> seen = new HashSet<>();
        List<Expense> toSave = new ArrayList<>();
        int duplicateCount = 0;
        for (int index = 0; index < request.transactions().size(); index++) {
            ExpenseImportRequest row = request.transactions().get(index);
            String key = fingerprints.get(index);
            if (existing.contains(key) || !seen.add(key)) {
                duplicateCount++;
                continue;
            }
            Expense expense = new Expense(userId, row.description(), row.category(), row.amount(), row.paymentMethod(), row.date());
            expense.setImportFingerprint(key);
            toSave.add(expense);
        }
        List<Expense> saved = expenseRepository.saveAll(toSave);
        return new ExpenseImportResult(saved.size(), duplicateCount, saved);
    }

    private static String readFingerprint(Expense expense) {
        return expense.getImportFingerprint();
    }

    private static String fingerprint(ExpenseImportRequest row) {
        String identity = row.sourceTransactionId() != null && !row.sourceTransactionId().isBlank()
                ? "source:" + normalize(row.sourceTransactionId())
                : String.join("|", row.date().toString(), row.amount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                        normalize(row.description()), normalize(row.paymentMethod()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<Expense> getAllExpenses(Long userId) {
        return expenseRepository.findAllByUserIdOrderByDateDesc(userId);
    }

    @Transactional(readOnly = true)
    public ExpensePageResponse getExpensePage(Long userId, int page, int size, String category, String search,
                                               java.time.LocalDate startDate, java.time.LocalDate endDate) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be non-negative and size must be between 1 and 100");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be on or before end date");
        }
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        Page<Expense> result = expenseRepository.findPageByUser(userId, normalizedCategory, normalizedSearch,
                startDate, endDate, PageRequest.of(page, size, Sort.by(Sort.Order.desc("date"), Sort.Order.desc("id"))));
        return new ExpensePageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public Expense getExpense(Long id, Long userId) {
        return findExpense(id, userId);
    }

    public ExpenseCreateResult addExpense(ExpenseRequest request, Long userId, String idempotencyKey) {
        if (idempotencyKey != null) {
            userAccountRepository.findByIdForUpdate(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account was not found"));
            String payloadHash = payloadHash(request);
            Optional<Expense> previous = expenseRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (previous.isPresent()) {
                Expense saved = previous.get();
                if (!payloadHash.equals(readIdempotencyHash(saved))) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "This idempotency key was already used with a different expense request");
                }
                return new ExpenseCreateResult(saved, true);
            }

            Expense expense = createExpense(request, userId);
            expense.setIdempotency(idempotencyKey, payloadHash);
            return new ExpenseCreateResult(expenseRepository.save(expense), false);
        }

        return new ExpenseCreateResult(createAndSave(request, userId), false);
    }

    public Expense addExpense(ExpenseRequest request, Long userId) {
        return addExpense(request, userId, null).expense();
    }

    private Expense createAndSave(ExpenseRequest request, Long userId) {
        return expenseRepository.save(createExpense(request, userId));
    }

    private Expense createExpense(ExpenseRequest request, Long userId) {
        Expense expense = new Expense(userId, request.getDescription(), request.getCategory(), request.getAmount(), request.getPaymentMethod(), request.getDate());
        expense.updateSplitDetails(request.getSplitDetails());
        return expense;
    }

    private static String readIdempotencyHash(Expense expense) {
        return expense.getIdempotencyPayloadHash();
    }

    private static String payloadHash(ExpenseRequest request) {
        List<String> fields = List.of(request.getDescription(), request.getCategory(),
                request.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString(), request.getPaymentMethod(),
                request.getDate().toString(), request.getSplitDetails() == null ? "" : request.getSplitDetails());
        StringBuilder canonical = new StringBuilder();
        for (String field : fields) canonical.append(field.length()).append(':').append(field);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public Expense updateExpense(Long id, ExpenseRequest request, Long userId) {
        Expense expense = findExpense(id, userId);
        expense.updateDetails(request.getDescription(), request.getCategory(), request.getAmount(), request.getPaymentMethod(), request.getDate());
        expense.updateSplitDetails(request.getSplitDetails());
        return expenseRepository.save(expense);
    }

    public void deleteExpense(Long id, Long userId) {
        Expense expense = findExpense(id, userId);
        expenseRepository.delete(expense);
    }

    private Expense findExpense(Long id, Long userId) {
        return expenseRepository.findByIdAndUserId(id, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense " + id + " was not found"));
    }
}
