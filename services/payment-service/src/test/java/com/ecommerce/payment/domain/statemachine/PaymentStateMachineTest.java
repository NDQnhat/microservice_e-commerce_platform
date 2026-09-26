package com.ecommerce.payment.domain.statemachine;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStateMachineTest {

    private PaymentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new PaymentStateMachine();
    }

    @Test
    @DisplayName("PAY-T01: Initial creation allows transition to INITIATED")
    void testInitialTransition() {
        assertThat(stateMachine.isValidTransition(null, PaymentStatus.INITIATED)).isTrue();
        stateMachine.validateTransition(null, PaymentStatus.INITIATED);
    }

    @Test
    @DisplayName("Initial transition to non-INITIATED is rejected")
    void testInvalidInitialTransition() {
        assertThat(stateMachine.isValidTransition(null, PaymentStatus.SUCCEEDED)).isFalse();
        assertThatThrownBy(() -> stateMachine.validateTransition(null, PaymentStatus.SUCCEEDED))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("PAY-T02: INITIATED -> SUCCEEDED is valid")
    void testInitiatedToSucceeded() {
        assertThat(stateMachine.isValidTransition(PaymentStatus.INITIATED, PaymentStatus.SUCCEEDED)).isTrue();
        stateMachine.validateTransition(PaymentStatus.INITIATED, PaymentStatus.SUCCEEDED);
    }

    @Test
    @DisplayName("PAY-T03: INITIATED -> FAILED is valid")
    void testInitiatedToFailed() {
        assertThat(stateMachine.isValidTransition(PaymentStatus.INITIATED, PaymentStatus.FAILED)).isTrue();
        stateMachine.validateTransition(PaymentStatus.INITIATED, PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("PAY-T04: INITIATED -> TIMEOUT is valid")
    void testInitiatedToTimeout() {
        assertThat(stateMachine.isValidTransition(PaymentStatus.INITIATED, PaymentStatus.TIMEOUT)).isTrue();
        stateMachine.validateTransition(PaymentStatus.INITIATED, PaymentStatus.TIMEOUT);
    }

    @Test
    @DisplayName("Terminal state SUCCEEDED cannot transition to any other state (BR-001)")
    void testSucceededIsTerminal() {
        assertThat(stateMachine.isValidTransition(PaymentStatus.SUCCEEDED, PaymentStatus.INITIATED)).isFalse();
        assertThat(stateMachine.isValidTransition(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED)).isFalse();
        assertThat(stateMachine.isValidTransition(PaymentStatus.SUCCEEDED, PaymentStatus.TIMEOUT)).isFalse();

        assertThatThrownBy(() -> stateMachine.validateTransition(PaymentStatus.SUCCEEDED, PaymentStatus.FAILED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    @DisplayName("Terminal state FAILED cannot transition to any other state (BR-001)")
    void testFailedIsTerminal() {
        assertThat(stateMachine.isValidTransition(PaymentStatus.FAILED, PaymentStatus.INITIATED)).isFalse();
        assertThat(stateMachine.isValidTransition(PaymentStatus.FAILED, PaymentStatus.SUCCEEDED)).isFalse();
        assertThat(stateMachine.isValidTransition(PaymentStatus.FAILED, PaymentStatus.TIMEOUT)).isFalse();

        assertThatThrownBy(() -> stateMachine.validateTransition(PaymentStatus.FAILED, PaymentStatus.SUCCEEDED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("terminal state");
    }

    @Test
    @DisplayName("Terminal state TIMEOUT cannot transition to any other state (BR-001)")
    void testTimeoutIsTerminal() {
        assertThat(stateMachine.isValidTransition(PaymentStatus.TIMEOUT, PaymentStatus.INITIATED)).isFalse();
        assertThat(stateMachine.isValidTransition(PaymentStatus.TIMEOUT, PaymentStatus.SUCCEEDED)).isFalse();
        assertThat(stateMachine.isValidTransition(PaymentStatus.TIMEOUT, PaymentStatus.FAILED)).isFalse();

        assertThatThrownBy(() -> stateMachine.validateTransition(PaymentStatus.TIMEOUT, PaymentStatus.SUCCEEDED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("terminal state");
    }
}
