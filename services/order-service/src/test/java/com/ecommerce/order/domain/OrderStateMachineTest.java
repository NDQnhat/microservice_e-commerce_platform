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
    @DisplayName("BR-001: CANCELLED can never transition to any other state")
    void shouldRejectTransitionFromCancelled() {
        for (OrderStatus target : OrderStatus.values()) {
            assertThatThrownBy(() -> stateMachine.validateTransition(OrderStatus.CANCELLED, target))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("BR-001");
        }
    }

    @Test
    @DisplayName("BR-006: PACKING cannot transition to CANCELLED")
    void shouldRejectCancellationAfterPacking() {
        assertThatThrownBy(() -> stateMachine.validateTransition(OrderStatus.PACKING, OrderStatus.CANCELLED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-006");
    }

    @Test
    @DisplayName("BR-007: Intermediate states cannot be skipped (e.g. RESERVED -> COMPLETED)")
    void shouldRejectSkippingIntermediateStates() {
        assertThatThrownBy(() -> stateMachine.validateTransition(OrderStatus.RESERVED, OrderStatus.COMPLETED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-007");
    }
}
