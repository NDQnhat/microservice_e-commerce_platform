package com.ecommerce.order.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    RESERVED,
    PAID,
    PACKING,
    SHIPPED,
    COMPLETED,
    PAYMENT_FAILED,
    EXPIRED,
    CANCELLED;

    private static final Set<OrderStatus> TERMINAL_STATES = EnumSet.of(COMPLETED, PAYMENT_FAILED, EXPIRED, CANCELLED);

    public boolean isTerminal() {
        return TERMINAL_STATES.contains(this);
    }
}
