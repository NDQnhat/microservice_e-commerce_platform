package com.ecommerce.inventory.domain;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.InvalidStateException;
import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.model.InventoryReservation;
import com.ecommerce.inventory.domain.model.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("reserve updates reserved quantity and updatedAt")
    void reserve_UpdatesReservedQuantity() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 50);
        inventory.reserve(20);

        assertThat(inventory.getQuantityReserved()).isEqualTo(20);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(30);
    }

    @Test
    @DisplayName("reserve throws BusinessRuleException when requested exceeds available")
    void reserve_ThrowsException_WhenExceedsAvailable() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10);
        inventory.setQuantityReserved(5); // available = 5

        assertThatThrownBy(() -> inventory.reserve(6))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getRuleId()).isEqualTo("BR-004"));
    }

    @Test
    @DisplayName("release restores reserved quantity")
    void release_RestoresReservedQuantity() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 50);
        inventory.setQuantityReserved(20);

        inventory.release(15);
        assertThat(inventory.getQuantityReserved()).isEqualTo(5);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(45);
    }

    @Test
    @DisplayName("commit deducts from onHand and reserved")
    void commit_DeductsOnHandAndReserved() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 50);
        inventory.setQuantityReserved(20);

        inventory.commit(20);
        assertThat(inventory.getQuantityOnHand()).isEqualTo(30);
        assertThat(inventory.getQuantityReserved()).isEqualTo(0);
        assertThat(inventory.getQuantityAvailable()).isEqualTo(30);
    }

    @Test
    @DisplayName("adjust updates onHand with positive delta")
    void adjust_PositiveDelta() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10);
        inventory.adjust(15);

        assertThat(inventory.getQuantityOnHand()).isEqualTo(25);
    }

    @Test
    @DisplayName("adjust throws BusinessRuleException when new onHand is negative")
    void adjust_ThrowsException_WhenNegativeOnHand() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 10);

        assertThatThrownBy(() -> inventory.adjust(-15))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getRuleId()).isEqualTo("BR-004"));
    }

    @Test
    @DisplayName("adjust throws BusinessRuleException when onHand drops below quantityReserved")
    void adjust_ThrowsException_WhenOnHandLessThanReserved() {
        Inventory inventory = new Inventory(UUID.randomUUID(), 20);
        inventory.setQuantityReserved(15);

        assertThatThrownBy(() -> inventory.adjust(-10)) // 20 - 10 = 10 < 15 reserved
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getRuleId()).isEqualTo("BR-004"));
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

    @Test
    @DisplayName("InventoryReservation state transition helpers work and reject invalid transitions")
    void reservationTransitions_Helpers() {
        InventoryReservation res = new InventoryReservation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                5,
                Instant.now().plus(15, ChronoUnit.MINUTES)
        );

        assertThat(res.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(res.isExpired(Instant.now())).isFalse();

        res.consume();
        assertThat(res.getStatus()).isEqualTo(ReservationStatus.CONSUMED);

        // Transitioning from CONSUMED throws InvalidStateException
        assertThatThrownBy(res::release)
                .isInstanceOf(InvalidStateException.class);
    }
}
