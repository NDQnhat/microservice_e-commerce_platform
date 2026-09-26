package com.ecommerce.payment.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_transaction")
public class PaymentTransaction {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "attempted_at", nullable = false, updatable = false)
    private Instant attemptedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "evidence_reference")
    private String evidenceReference;

    @Column(name = "reconciliation_reason")
    private String reconciliationReason;

    public PaymentTransaction() {
        this.id = UUID.randomUUID();
        this.status = PaymentStatus.INITIATED;
        this.attemptedAt = Instant.now();
    }

    public PaymentTransaction(UUID orderId, BigDecimal amount) {
        this.id = UUID.randomUUID();
        this.orderId = orderId;
        this.amount = amount;
        this.status = PaymentStatus.INITIATED;
        this.attemptedAt = Instant.now();
    }

    public void markSucceeded(String providerReference) {
        this.providerReference = providerReference;
        this.status = PaymentStatus.SUCCEEDED;
        this.confirmedAt = Instant.now();
    }

    public void markFailed(String providerReference) {
        this.providerReference = providerReference;
        this.status = PaymentStatus.FAILED;
        this.confirmedAt = Instant.now();
    }

    public void markTimeout() {
        this.status = PaymentStatus.TIMEOUT;
        this.confirmedAt = Instant.now();
    }

    public void markReconciled(String evidenceReference, String reason) {
        this.evidenceReference = evidenceReference;
        this.reconciliationReason = reason;
        this.status = PaymentStatus.SUCCEEDED;
        this.confirmedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public Instant getAttemptedAt() {
        return attemptedAt;
    }

    public void setAttemptedAt(Instant attemptedAt) {
        this.attemptedAt = attemptedAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getEvidenceReference() {
        return evidenceReference;
    }

    public void setEvidenceReference(String evidenceReference) {
        this.evidenceReference = evidenceReference;
    }

    public String getReconciliationReason() {
        return reconciliationReason;
    }

    public void setReconciliationReason(String reconciliationReason) {
        this.reconciliationReason = reconciliationReason;
    }
}
