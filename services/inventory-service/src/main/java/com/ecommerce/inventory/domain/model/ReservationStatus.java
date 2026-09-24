package com.ecommerce.inventory.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum ReservationStatus {
    ACTIVE,
    CONSUMED,
    RELEASED,
    EXPIRED;

    private static final Set<ReservationStatus> TERMINAL_STATES = EnumSet.of(CONSUMED, RELEASED, EXPIRED);

    public boolean isTerminal() {
        return TERMINAL_STATES.contains(this);
    }

    public boolean canTransitionTo(ReservationStatus target) {
        if (isTerminal()) {
            return false;
        }
        return this == ACTIVE && (target == CONSUMED || target == RELEASED || target == EXPIRED);
    }
}
