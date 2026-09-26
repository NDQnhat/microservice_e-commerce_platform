package com.ecommerce.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentTransactionDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("provider_reference")
    private String providerReference;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("status")
    private String status;

    @JsonProperty("attempted_at")
    private Instant attemptedAt;

    @JsonProperty("confirmed_at")
    private Instant confirmedAt;

    @JsonProperty("evidence_reference")
    private String evidenceReference;

    @JsonProperty("reconciliation_reason")
    private String reconciliationReason;

    public PaymentTransactionDto() {
    }

    public PaymentTransactionDto(UUID id, UUID orderId, String providerReference,
                                 BigDecimal amount, String status, Instant attemptedAt,
                                 Instant confirmedAt) {
        this(id, orderId, providerReference, amount, status, attemptedAt, confirmedAt, null, null);
    }

    public PaymentTransactionDto(UUID id, UUID orderId, String providerReference,
                                 BigDecimal amount, String status, Instant attemptedAt,
                                 Instant confirmedAt, String evidenceReference,
                                 String reconciliationReason) {
        this.id = id;
        this.orderId = orderId;
        this.providerReference = providerReference;
        this.amount = amount;
        this.status = status;
        this.attemptedAt = attemptedAt;
        this.confirmedAt = confirmedAt;
        this.evidenceReference = evidenceReference;
        this.reconciliationReason = reconciliationReason;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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
