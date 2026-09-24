package com.ecommerce.exception.domain.model;

import com.ecommerce.common.error.InvalidStateException;
import java.util.Set;

public enum ExceptionRecordStatus {
    OPEN,
    INVESTIGATING,
    RESOLVED,
    IGNORED;

    public void validateTransitionTo(ExceptionRecordStatus targetStatus) {
        if (this == targetStatus) {
            return;
        }

        switch (this) {
            case OPEN -> {
                if (targetStatus != INVESTIGATING && targetStatus != RESOLVED && targetStatus != IGNORED) {
                    throw new InvalidStateException("Cannot transition exception record from " + this + " to " + targetStatus);
                }
            }
            case INVESTIGATING -> {
                if (targetStatus != RESOLVED && targetStatus != IGNORED) {
                    throw new InvalidStateException("Cannot transition exception record from " + this + " to " + targetStatus);
                }
            }
            case RESOLVED, IGNORED -> throw new InvalidStateException("Terminal exception status " + this + " cannot transition to " + targetStatus);
        }
    }
}
