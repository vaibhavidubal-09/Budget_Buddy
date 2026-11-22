package com.budgetbuddy.api.expenses;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "expenses", indexes = {
        @Index(name = "idx_expenses_user_date", columnList = "user_id,date")
})
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 120)
    private String description;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "split_details", columnDefinition = "TEXT")
    private String splitDetails;

    @Column(name = "import_fingerprint", length = 64)
    private String importFingerprint;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "idempotency_payload_hash", length = 64)
    private String idempotencyPayloadHash;

    protected Expense() {
        // JPA uses this constructor when it reads a row from MySQL.
    }

    public Expense(Long userId, String description, String category, BigDecimal amount, String paymentMethod, LocalDate date) {
        this.userId = userId;
        updateDetails(description, category, amount, paymentMethod, date);
    }

    public void updateDetails(String description, String category, BigDecimal amount, String paymentMethod, LocalDate date) {
        this.description = description;
        this.category = category;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.date = date;
    }

    public void updateSplitDetails(String splitDetails) { this.splitDetails = splitDetails; }
    public void setImportFingerprint(String importFingerprint) { this.importFingerprint = importFingerprint; }
    @JsonIgnore
    String getImportFingerprint() { return importFingerprint; }
    public void setIdempotency(String key, String payloadHash) {
        this.idempotencyKey = key;
        this.idempotencyPayloadHash = payloadHash;
    }
    @JsonIgnore
    String getIdempotencyPayloadHash() { return idempotencyPayloadHash; }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public LocalDate getDate() { return date; }
    public String getSplitDetails() { return splitDetails; }
}
