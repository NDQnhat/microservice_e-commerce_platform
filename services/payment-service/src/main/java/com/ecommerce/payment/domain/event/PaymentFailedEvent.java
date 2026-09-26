package com.ecommerce.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID transactionId,
        UUID orderId,
        String providerReference,
        BigDecimal amount,
        String reason,
        Instant timestamp
) {
}
