package com.ecommerce.fulfillment.domain;

import com.ecommerce.fulfillment.domain.model.Shipment;
import com.ecommerce.fulfillment.domain.model.ShipmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShipmentStateTest {

    @Test
    @DisplayName("New shipment starts in PACKING state")
    void shouldStartInPacking() {
        UUID orderId = UUID.randomUUID();
        Shipment shipment = new Shipment(orderId);

        assertThat(shipment.getId()).isNotNull();
        assertThat(shipment.getOrderId()).isEqualTo(orderId);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.PACKING);
        assertThat(shipment.getPackedAt()).isNotNull();
    }

    @Test
    @DisplayName("Shipment transitions correctly from PACKING -> SHIPPED -> DELIVERED")
    void shouldValidateShipmentTransitions() {
        assertThat(ShipmentStatus.PACKING.canTransitionTo(ShipmentStatus.SHIPPED)).isTrue();
        assertThat(ShipmentStatus.SHIPPED.canTransitionTo(ShipmentStatus.DELIVERED)).isTrue();

        // Cannot skip directly from PACKING to DELIVERED
        assertThat(ShipmentStatus.PACKING.canTransitionTo(ShipmentStatus.DELIVERED)).isFalse();
        // Cannot revert
        assertThat(ShipmentStatus.DELIVERED.canTransitionTo(ShipmentStatus.SHIPPED)).isFalse();
    }
}
