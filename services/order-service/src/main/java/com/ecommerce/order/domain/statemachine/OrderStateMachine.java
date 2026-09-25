package com.ecommerce.order.domain.statemachine;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.order.domain.model.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        // ORD-T02, ORD-T03, ORD-T04, ORD-T05
        VALID_TRANSITIONS.put(OrderStatus.RESERVED, EnumSet.of(
                OrderStatus.PAID,
                OrderStatus.PAYMENT_FAILED,
                OrderStatus.EXPIRED,
                OrderStatus.CANCELLED
        ));

        // ORD-T06, ORD-T07
        VALID_TRANSITIONS.put(OrderStatus.PAID, EnumSet.of(
                OrderStatus.PACKING,
                OrderStatus.CANCELLED
        ));

        // ORD-T08
        VALID_TRANSITIONS.put(OrderStatus.PACKING, EnumSet.of(
                OrderStatus.SHIPPED
        ));

        // ORD-T09
        VALID_TRANSITIONS.put(OrderStatus.SHIPPED, EnumSet.of(
                OrderStatus.COMPLETED
        ));

        // Terminal states have no outbound transitions (BR-001)
        VALID_TRANSITIONS.put(OrderStatus.COMPLETED, Collections.emptySet());
        VALID_TRANSITIONS.put(OrderStatus.PAYMENT_FAILED, Collections.emptySet());
        VALID_TRANSITIONS.put(OrderStatus.EXPIRED, Collections.emptySet());
        VALID_TRANSITIONS.put(OrderStatus.CANCELLED, Collections.emptySet());
    }

    public boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (from == null) {
            return to == OrderStatus.RESERVED; // ORD-T01
        }
        Set<OrderStatus> allowedTargets = VALID_TRANSITIONS.get(from);
        return allowedTargets != null && allowedTargets.contains(to);
    }

    public void validateTransition(OrderStatus from, OrderStatus to) {
        if (!isValidTransition(from, to)) {
            if (from == OrderStatus.CANCELLED) {
                throw new BusinessRuleException("BR-001", "An order can never transition from CANCELLED to any other state.");
            }
            if (to == OrderStatus.CANCELLED && (from == OrderStatus.PACKING || from == OrderStatus.SHIPPED || from == OrderStatus.COMPLETED)) {
                throw new BusinessRuleException("BR-006", String.format("Cancellation cutoff exceeded; order is already in %s state.", from));
            }
            throw new BusinessRuleException("BR-007", String.format("Invalid order state transition from %s to %s.", from, to));
        }
    }
}
