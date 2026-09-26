package com.ecommerce.fulfillment.domain.statemachine;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.fulfillment.domain.model.ShipmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShipmentStateMachineTest {

    private ShipmentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new ShipmentStateMachine();
    }

    @Test
    @DisplayName("Valid transition: null -> PACKING when order is PAID")
    void shouldAllowNullToPackingWhenOrderPaid() {
        stateMachine.validateTransition(null, ShipmentStatus.PACKING, null, null, "PAID");
        assertThat(stateMachine.isValidTransition(null, ShipmentStatus.PACKING)).isTrue();
    }

    @Test
    @DisplayName("Valid transition: null -> PACKING when order is PACKING")
    void shouldAllowNullToPackingWhenOrderPacking() {
        stateMachine.validateTransition(null, ShipmentStatus.PACKING, null, null, "PACKING");
    }

    @Test
    @DisplayName("Invalid transition: null -> PACKING rejected when order is RESERVED (BR-011)")
    void shouldRejectNullToPackingWhenOrderReserved() {
        assertThatThrownBy(() ->
                stateMachine.validateTransition(null, ShipmentStatus.PACKING, null, null, "RESERVED"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011")
                .hasMessageContaining("RESERVED");
    }

    @Test
    @DisplayName("Invalid transition: null -> PACKING rejected when order is CANCELLED (BR-011)")
    void shouldRejectNullToPackingWhenOrderCancelled() {
        assertThatThrownBy(() ->
                stateMachine.validateTransition(null, ShipmentStatus.PACKING, null, null, "CANCELLED"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011");
    }

    @Test
    @DisplayName("Invalid transition: null -> SHIPPED rejected")
    void shouldRejectNullToShipped() {
        assertThatThrownBy(() ->
                stateMachine.validateTransition(null, ShipmentStatus.SHIPPED, "Carrier", "TRK123", "PAID"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011");
    }

    @Test
    @DisplayName("Valid transition: PACKING -> SHIPPED with valid carrier and tracking (BR-011)")
    void shouldAllowPackingToShippedWithCarrierAndTracking() {
        stateMachine.validateTransition(ShipmentStatus.PACKING, ShipmentStatus.SHIPPED,
                "Vietnam Post", "VN123456789", "PACKING");
        assertThat(stateMachine.isValidTransition(ShipmentStatus.PACKING, ShipmentStatus.SHIPPED)).isTrue();
    }

    @Test
    @DisplayName("Invalid transition: PACKING -> SHIPPED fails if carrier_name is missing (BR-011)")
    void shouldRejectPackingToShippedWithoutCarrier() {
        assertThatThrownBy(() ->
                stateMachine.validateTransition(ShipmentStatus.PACKING, ShipmentStatus.SHIPPED,
                        "", "VN123456789", "PACKING"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011")
                .hasMessageContaining("Tracking code and carrier name must be provided");
    }

    @Test
    @DisplayName("Invalid transition: PACKING -> SHIPPED fails if tracking_code is missing (BR-011)")
    void shouldRejectPackingToShippedWithoutTracking() {
        assertThatThrownBy(() ->
                stateMachine.validateTransition(ShipmentStatus.PACKING, ShipmentStatus.SHIPPED,
                        "Vietnam Post", "  ", "PACKING"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011")
                .hasMessageContaining("Tracking code and carrier name must be provided");
    }

    @Test
    @DisplayName("Invalid transition: PACKING -> SHIPPED fails if order has not reached PACKING (BR-011)")
    void shouldRejectPackingToShippedIfOrderNotPacking() {
        assertThatThrownBy(() ->
                stateMachine.validateTransition(ShipmentStatus.PACKING, ShipmentStatus.SHIPPED,
                        "Vietnam Post", "VN123456", "RESERVED"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011")
                .hasMessageContaining("Cannot ship order: order has not reached PACKING state");
    }

    @Test
    @DisplayName("Valid transition: SHIPPED -> DELIVERED (SHP-T03)")
    void shouldAllowShippedToDelivered() {
        stateMachine.validateTransition(ShipmentStatus.SHIPPED, ShipmentStatus.DELIVERED,
                "Vietnam Post", "VN123456", "SHIPPED");
        assertThat(stateMachine.isValidTransition(ShipmentStatus.SHIPPED, ShipmentStatus.DELIVERED)).isTrue();
    }

    @Test
    @DisplayName("Forbidden transition: PACKING -> DELIVERED skip rejected (BR-011)")
    void shouldRejectDirectPackingToDelivered() {
        assertThatThrownBy(() ->
                stateMachine.validateTransition(ShipmentStatus.PACKING, ShipmentStatus.DELIVERED,
                        "Carrier", "TRK123", "PACKING"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011")
                .hasMessageContaining("Direct transition from PACKING to DELIVERED is forbidden");
        assertThat(stateMachine.isValidTransition(ShipmentStatus.PACKING, ShipmentStatus.DELIVERED)).isFalse();
    }

    @Test
    @DisplayName("Forbidden transition: Reversal SHIPPED -> PACKING rejected (BR-011)")
    void shouldRejectShippedToPackingReversal() {
        assertThatThrownBy(() ->
                stateMachine.validateTransition(ShipmentStatus.SHIPPED, ShipmentStatus.PACKING,
                        null, null, "SHIPPED"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011")
                .hasMessageContaining("Reversing shipment status");
    }

    @Test
    @DisplayName("Forbidden transition: Outbound transition from DELIVERED terminal state rejected (BR-011)")
    void shouldRejectOutboundFromDeliveredTerminal() {
        assertThatThrownBy(() ->
                stateMachine.validateTransition(ShipmentStatus.DELIVERED, ShipmentStatus.SHIPPED,
                        null, null, "COMPLETED"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011")
                .hasMessageContaining("Cannot transition out of DELIVERED terminal state");

        assertThatThrownBy(() ->
                stateMachine.validateTransition(ShipmentStatus.DELIVERED, ShipmentStatus.PACKING,
                        null, null, "COMPLETED"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011");

        assertThat(stateMachine.isValidTransition(ShipmentStatus.DELIVERED, ShipmentStatus.SHIPPED)).isFalse();
    }

    @Test
    @DisplayName("Idempotent transition: Same status is valid")
    void shouldAllowSameStatusIdempotent() {
        stateMachine.validateTransition(ShipmentStatus.PACKING, ShipmentStatus.PACKING, null, null, "PACKING");
        stateMachine.validateTransition(ShipmentStatus.SHIPPED, ShipmentStatus.SHIPPED, "Carrier", "TRK", "SHIPPED");
        stateMachine.validateTransition(ShipmentStatus.DELIVERED, ShipmentStatus.DELIVERED, null, null, "COMPLETED");
    }
}
