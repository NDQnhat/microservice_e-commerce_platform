package com.ecommerce.payment.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum PaymentStatus {
    INITIATED,
    SUCCEEDED,
    FAILED,
    TIMEOUT;

    private static final Set<PaymentStatus> TERMINAL_STATES = EnumSet.of(SUCCEEDED, FAILED, TIMEOUT);

    public boolean isTerminal() {
        return TERMINAL_STATES.contains(this);
    }
}
