package com.ecommerce.inventory.domain;

import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.model.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryDomainTest {

    @Test
    @DisplayName("Inventory computes available quantity as onHand minus reserved")
    void shouldComputeQuantityAvailable() {
        UUID skuId = UUID.randomUUID();
        Inventory inventory = new Inventory(skuId, 100);
        inventory.setQuantityReserved(25);

        assertThat(inventory.getQuantityAvailable()).isEqualTo(75);
    }

    @Test
    @DisplayName("Reservation state machine allows ACTIVE to transition to CONSUMED, RELEASED, or EXPIRED")
    void shouldValidateReservationTransitions() {
        assertThat(ReservationStatus.ACTIVE.canTransitionTo(ReservationStatus.CONSUMED)).isTrue();
        assertThat(ReservationStatus.ACTIVE.canTransitionTo(ReservationStatus.RELEASED)).isTrue();
        assertThat(ReservationStatus.ACTIVE.canTransitionTo(ReservationStatus.EXPIRED)).isTrue();

        // Terminal states cannot transition
        assertThat(ReservationStatus.CONSUMED.canTransitionTo(ReservationStatus.RELEASED)).isFalse();
        assertThat(ReservationStatus.RELEASED.canTransitionTo(ReservationStatus.ACTIVE)).isFalse();
        assertThat(ReservationStatus.EXPIRED.canTransitionTo(ReservationStatus.ACTIVE)).isFalse();
    }
}
