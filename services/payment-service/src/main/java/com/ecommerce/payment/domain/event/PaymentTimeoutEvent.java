package com.ecommerce.payment.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentTimeoutEvent(
        UUID transactionId,
        UUID orderId,
        BigDecimal amount,
        Instant timestamp
) {
}
