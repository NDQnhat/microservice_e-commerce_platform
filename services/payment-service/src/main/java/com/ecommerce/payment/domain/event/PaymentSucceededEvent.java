package com.ecommerce.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentSucceededEvent(
        UUID transactionId,
        UUID orderId,
        String providerReference,
        BigDecimal amount,
        Instant timestamp,
        boolean reconciled,
        String evidenceReference,
        String reason
) {
    public PaymentSucceededEvent(UUID transactionId, UUID orderId, String providerReference, BigDecimal amount, Instant timestamp) {
        this(transactionId, orderId, providerReference, amount, timestamp, false, null, null);
    }
}
