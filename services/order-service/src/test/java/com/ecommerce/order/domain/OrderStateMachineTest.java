package com.ecommerce.order.domain;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.statemachine.OrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStateMachineTest {

    private OrderStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new OrderStateMachine();
    }

    @Test
    @DisplayName("ORD-T01 to ORD-T09 valid sequence: null -> RESERVED -> PAID -> PACKING -> SHIPPED -> COMPLETED")
    void shouldAllowHappyPathTransitions() {
        assertThat(stateMachine.isValidTransition(null, OrderStatus.RESERVED)).isTrue();
        assertThat(stateMachine.isValidTransition(OrderStatus.RESERVED, OrderStatus.PAID)).isTrue();
        assertThat(stateMachine.isValidTransition(OrderStatus.PAID, OrderStatus.PACKING)).isTrue();
        assertThat(stateMachine.isValidTransition(OrderStatus.PACKING, OrderStatus.SHIPPED)).isTrue();
        assertThat(stateMachine.isValidTransition(OrderStatus.SHIPPED, OrderStatus.COMPLETED)).isTrue();
    }

    @Test
    @DisplayName("ORD-T03, T04, T05, T07 valid alternative & cancellation branches before cutoff")
    void shouldAllowAlternativeTransitionsBeforeCutoff() {
        assertThat(stateMachine.isValidTransition(OrderStatus.RESERVED, OrderStatus.PAYMENT_FAILED)).isTrue();
        assertThat(stateMachine.isValidTransition(OrderStatus.RESERVED, OrderStatus.EXPIRED)).isTrue();
        assertThat(stateMachine.isValidTransition(OrderStatus.RESERVED, OrderStatus.CANCELLED)).isTrue();
        assertThat(stateMachine.isValidTransition(OrderStatus.PAID, OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    @DisplayName("BR-001: CANCELLED can never transition to any other state (strictly terminal)")
    void shouldRejectTransitionFromCancelled() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThatThrownBy(() -> stateMachine.validateTransition(OrderStatus.CANCELLED, target))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("BR-001");
        }
    }

    @Test
    @DisplayName("BR-006: PACKING, SHIPPED, and COMPLETED cannot transition to CANCELLED")
    void shouldRejectCancellationAfterPacking() {
        for (OrderStatus postCutoff : new OrderStatus[]{OrderStatus.PACKING, OrderStatus.SHIPPED, OrderStatus.COMPLETED}) {
            assertThatThrownBy(() -> stateMachine.validateTransition(postCutoff, OrderStatus.CANCELLED))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("BR-006");
        }
    }

    @Test
    @DisplayName("BR-007: Intermediate states cannot be skipped (e.g. RESERVED -> COMPLETED, RESERVED -> PACKING)")
    void shouldRejectSkippingIntermediateStates() {
        assertThatThrownBy(() -> stateMachine.validateTransition(OrderStatus.RESERVED, OrderStatus.COMPLETED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-007");

        assertThatThrownBy(() -> stateMachine.validateTransition(OrderStatus.RESERVED, OrderStatus.PACKING))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-007");

        assertThatThrownBy(() -> stateMachine.validateTransition(OrderStatus.PAID, OrderStatus.SHIPPED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-007");
    }

    @Test
    @DisplayName("Terminal States: COMPLETED, PAYMENT_FAILED, EXPIRED, CANCELLED are terminal")
    void verifyTerminalStates() {
        assertThat(OrderStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(OrderStatus.PAYMENT_FAILED.isTerminal()).isTrue();
        assertThat(OrderStatus.EXPIRED.isTerminal()).isTrue();
        assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();

        assertThat(OrderStatus.RESERVED.isTerminal()).isFalse();
        assertThat(OrderStatus.PAID.isTerminal()).isFalse();
        assertThat(OrderStatus.PACKING.isTerminal()).isFalse();
        assertThat(OrderStatus.SHIPPED.isTerminal()).isFalse();
    }
}
