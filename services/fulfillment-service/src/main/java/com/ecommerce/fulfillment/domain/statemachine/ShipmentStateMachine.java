package com.ecommerce.fulfillment.domain.statemachine;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.fulfillment.domain.model.ShipmentStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class ShipmentStateMachine {

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> VALID_TRANSITIONS = new EnumMap<>(ShipmentStatus.class);

    static {
        // SHP-T02
        VALID_TRANSITIONS.put(ShipmentStatus.PACKING, EnumSet.of(ShipmentStatus.SHIPPED));
        // SHP-T03
        VALID_TRANSITIONS.put(ShipmentStatus.SHIPPED, EnumSet.of(ShipmentStatus.DELIVERED));
        // DELIVERED is terminal
        VALID_TRANSITIONS.put(ShipmentStatus.DELIVERED, Collections.emptySet());
    }

    public boolean isValidTransition(ShipmentStatus from, ShipmentStatus to) {
        if (from == null) {
            return to == ShipmentStatus.PACKING;
        }
        if (from == to) {
            return true;
        }
        Set<ShipmentStatus> allowed = VALID_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public void validateTransition(ShipmentStatus from, ShipmentStatus to,
                                   String carrierName, String trackingCode) {
        validateTransition(from, to, carrierName, trackingCode, null);
    }

    public void validateTransition(ShipmentStatus from, ShipmentStatus to,
                                   String carrierName, String trackingCode, String orderStatus) {
        if (from == null) {
            if (to != ShipmentStatus.PACKING) {
                throw new BusinessRuleException("BR-011",
                        String.format("Initial shipment status must be PACKING, requested: %s", to));
            }
            if (orderStatus != null && !isEligibleForPacking(orderStatus)) {
                throw new BusinessRuleException("BR-011",
                        String.format("Cannot initiate shipment: order has not reached PAID/PACKING state (current: %s)", orderStatus));
            }
            return;
        }

        if (from == to) {
            return;
        }

        if (from == ShipmentStatus.DELIVERED) {
            throw new BusinessRuleException("BR-011",
                    "Cannot transition out of DELIVERED terminal state.");
        }

        if (from == ShipmentStatus.SHIPPED && to == ShipmentStatus.PACKING) {
            throw new BusinessRuleException("BR-011",
                    "Reversing shipment status from SHIPPED to PACKING is forbidden.");
        }

        if (from == ShipmentStatus.PACKING && to == ShipmentStatus.DELIVERED) {
            throw new BusinessRuleException("BR-011",
                    "Direct transition from PACKING to DELIVERED is forbidden.");
        }

        if (!isValidTransition(from, to)) {
            throw new BusinessRuleException("BR-011",
                    String.format("Invalid shipment transition from %s to %s.", from, to));
        }

        // Guard for SHP-T02: PACKING -> SHIPPED
        if (to == ShipmentStatus.SHIPPED) {
            if (carrierName == null || carrierName.trim().isEmpty() ||
                trackingCode == null || trackingCode.trim().isEmpty()) {
                throw new BusinessRuleException("BR-011",
                        "Tracking code and carrier name must be provided before SHIPPED state.");
            }

            if (orderStatus != null && !isEligibleForShipped(orderStatus)) {
                throw new BusinessRuleException("BR-011",
                        String.format("Cannot ship order: order has not reached PACKING state (current: %s)", orderStatus));
            }
        }
    }

    private boolean isEligibleForPacking(String orderStatus) {
        return "PAID".equalsIgnoreCase(orderStatus) || "PACKING".equalsIgnoreCase(orderStatus);
    }

    private boolean isEligibleForShipped(String orderStatus) {
        return "PACKING".equalsIgnoreCase(orderStatus) || "PAID".equalsIgnoreCase(orderStatus);
    }
}
