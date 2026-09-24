package com.ecommerce.payment.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentTransactionDto {

    private UUID id;
    private UUID orderId;
    private String providerReference;
    private BigDecimal amount;
    private String status;
    private Instant attemptedAt;
    private Instant confirmedAt;

    public PaymentTransactionDto() {
    }

    public PaymentTransactionDto(UUID id, UUID orderId, String providerReference,
                                 BigDecimal amount, String status, Instant attemptedAt,
                                 Instant confirmedAt) {
        this.id = id;
        this.orderId = orderId;
        this.providerReference = providerReference;
        this.amount = amount;
        this.status = status;
        this.attemptedAt = attemptedAt;
        this.confirmedAt = confirmedAt;
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
}
