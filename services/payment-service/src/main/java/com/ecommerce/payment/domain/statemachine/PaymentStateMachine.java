package com.ecommerce.payment.domain.statemachine;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.payment.domain.model.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> VALID_TRANSITIONS = new EnumMap<>(PaymentStatus.class);

    static {
        // PAY-T02, PAY-T03, PAY-T04
        VALID_TRANSITIONS.put(PaymentStatus.INITIATED, EnumSet.of(
                PaymentStatus.SUCCEEDED,
                PaymentStatus.FAILED,
                PaymentStatus.TIMEOUT
        ));

        // Terminal states cannot transition to any status (BR-001 / Section 9)
        VALID_TRANSITIONS.put(PaymentStatus.SUCCEEDED, Collections.emptySet());
        VALID_TRANSITIONS.put(PaymentStatus.FAILED, Collections.emptySet());
        VALID_TRANSITIONS.put(PaymentStatus.TIMEOUT, Collections.emptySet());
    }

    public boolean isValidTransition(PaymentStatus from, PaymentStatus to) {
        if (from == null) {
            return to == PaymentStatus.INITIATED; // PAY-T01
        }
        if (to == null) {
            return false;
        }
        Set<PaymentStatus> targets = VALID_TRANSITIONS.get(from);
        return targets != null && targets.contains(to);
    }

    public void validateTransition(PaymentStatus from, PaymentStatus to) {
        if (from == null) {
            if (to != PaymentStatus.INITIATED) {
                throw new BusinessRuleException("BR-007", "Initial payment transaction status must be INITIATED");
            }
            return;
        }

        if (from.isTerminal()) {
            throw new BusinessRuleException("BR-001",
                    String.format("Payment transaction is in terminal state %s and cannot transition to %s", from, to));
        }

        if (!isValidTransition(from, to)) {
            throw new BusinessRuleException("BR-007",
                    String.format("Invalid payment state transition from %s to %s", from, to));
        }
    }
}
