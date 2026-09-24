package com.ecommerce.payment.domain;

import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.model.PaymentTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDomainTest {

    @Test
    @DisplayName("New payment transaction is created in INITIATED status")
    void shouldCreateInitiatedTransaction() {
        UUID orderId = UUID.randomUUID();
        PaymentTransaction tx = new PaymentTransaction(orderId, new BigDecimal("500000.00"));

        assertThat(tx.getId()).isNotNull();
        assertThat(tx.getOrderId()).isEqualTo(orderId);
        assertThat(tx.getAmount()).isEqualTo(new BigDecimal("500000.00"));
        assertThat(tx.getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(tx.getStatus().isTerminal()).isFalse();
    }

    @Test
    @DisplayName("Terminal states for PaymentStatus are SUCCEEDED, FAILED, TIMEOUT")
    void terminalStatesCheck() {
        assertThat(PaymentStatus.SUCCEEDED.isTerminal()).isTrue();
        assertThat(PaymentStatus.FAILED.isTerminal()).isTrue();
        assertThat(PaymentStatus.TIMEOUT.isTerminal()).isTrue();
        assertThat(PaymentStatus.INITIATED.isTerminal()).isFalse();
    }
}
