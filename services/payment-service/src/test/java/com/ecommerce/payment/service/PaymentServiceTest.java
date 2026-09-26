package com.ecommerce.payment.service;

import com.ecommerce.common.error.AuthenticationFailedException;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.payment.api.dto.InitiatePaymentRequest;
import com.ecommerce.payment.api.dto.ManualReconcileRequest;
import com.ecommerce.payment.api.dto.PaymentCallbackRequest;
import com.ecommerce.payment.api.dto.PaymentTransactionDto;
import com.ecommerce.payment.client.ExceptionClient;
import com.ecommerce.payment.client.InventoryClient;
import com.ecommerce.payment.client.OrderClient;
import com.ecommerce.payment.domain.event.PaymentFailedEvent;
import com.ecommerce.payment.domain.event.PaymentSucceededEvent;
import com.ecommerce.payment.domain.event.PaymentTimeoutEvent;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.domain.model.PaymentTransaction;
import com.ecommerce.payment.domain.repository.PaymentTransactionRepository;
import com.ecommerce.payment.domain.statemachine.PaymentStateMachine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentTransactionRepository paymentRepository;

    @Mock
    private PaymentOutboxService outboxService;

    @Mock
    private OrderClient orderClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private ExceptionClient exceptionClient;

    private PaymentStateMachine stateMachine;
    private ObjectMapper objectMapper;
    private PaymentServiceImpl paymentService;

    private final String validSecret = "gateway-shared-secret-12345";

    @BeforeEach
    void setUp() {
        stateMachine = new PaymentStateMachine();
        objectMapper = new ObjectMapper();
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                stateMachine,
                outboxService,
                orderClient,
                inventoryClient,
                exceptionClient,
                objectMapper
        );
        ReflectionTestUtils.setField(paymentService, "configuredWebhookSecret", validSecret);
        ReflectionTestUtils.setField(paymentService, "defaultTimeoutMinutes", 15);
    }

    @Test
    @DisplayName("Initiate payment creates transaction in INITIATED status")
    void testInitiatePayment() {
        UUID orderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("250000.00");
        InitiatePaymentRequest request = new InitiatePaymentRequest(orderId, amount);

        when(paymentRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentTransactionDto dto = paymentService.initiatePayment(request);

        assertThat(dto.getOrderId()).isEqualTo(orderId);
        assertThat(dto.getAmount()).isEqualTo(amount);
        assertThat(dto.getStatus()).isEqualTo("INITIATED");
        verify(paymentRepository).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("Callback SUCCESS: transitions to SUCCEEDED, emits outbox, updates order to PAID")
    void testHandleCallback_Success() {
        UUID orderId = UUID.randomUUID();
        PaymentTransaction initiatedTx = new PaymentTransaction(orderId, new BigDecimal("100000.00"));

        when(paymentRepository.findByOrderIdOrderByAttemptedAtDesc(orderId))
                .thenReturn(List.of(initiatedTx));
        when(orderClient.getOrderStatus(orderId)).thenReturn("RESERVED");
        when(paymentRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentCallbackRequest request = new PaymentCallbackRequest(
                orderId, "GW-REF-001", "SUCCESS", new BigDecimal("100000.00"));

        PaymentTransactionDto response = paymentService.handleCallback(request, validSecret);

        assertThat(response.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(response.getProviderReference()).isEqualTo("GW-REF-001");
        verify(outboxService).recordEvent(anyString(), eq("PaymentSucceeded"), any(PaymentSucceededEvent.class));
        verify(orderClient).updateOrderStatus(eq(orderId), eq("PAID"), anyString());
        verifyNoInteractions(inventoryClient);
    }

    @Test
    @DisplayName("Callback FAILED: transitions to FAILED, releases inventory, updates order to PAYMENT_FAILED")
    void testHandleCallback_Failed() {
        UUID orderId = UUID.randomUUID();
        PaymentTransaction initiatedTx = new PaymentTransaction(orderId, new BigDecimal("100000.00"));

        when(paymentRepository.findByOrderIdOrderByAttemptedAtDesc(orderId))
                .thenReturn(List.of(initiatedTx));
        when(orderClient.getOrderStatus(orderId)).thenReturn("RESERVED");
        when(paymentRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentCallbackRequest request = new PaymentCallbackRequest(
                orderId, "GW-REF-FAIL", "FAILED", new BigDecimal("100000.00"));

        PaymentTransactionDto response = paymentService.handleCallback(request, validSecret);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        verify(outboxService).recordEvent(anyString(), eq("PaymentFailed"), any(PaymentFailedEvent.class));
        verify(inventoryClient).releaseStock(eq(orderId), anyString());
        verify(orderClient).updateOrderStatus(eq(orderId), eq("PAYMENT_FAILED"), anyString());
    }

    @Test
    @DisplayName("Callback with invalid webhook secret throws AuthenticationFailedException")
    void testHandleCallback_InvalidSecret() {
        UUID orderId = UUID.randomUUID();
        PaymentCallbackRequest request = new PaymentCallbackRequest(
                orderId, "GW-REF-001", "SUCCESS", new BigDecimal("100000.00"));

        assertThatThrownBy(() -> paymentService.handleCallback(request, "wrong-secret"))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessageContaining("signature/secret");

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(orderClient);
        verifyNoInteractions(outboxService);
    }

    @Test
    @DisplayName("Step 6: Idempotent callback retry returns existing transaction without duplicating side effects")
    void testHandleCallback_IdempotentRetry() {
        UUID orderId = UUID.randomUUID();
        PaymentTransaction existingSucceeded = new PaymentTransaction(orderId, new BigDecimal("100000.00"));
        existingSucceeded.markSucceeded("GW-REF-RETRY");

        when(paymentRepository.findByOrderIdOrderByAttemptedAtDesc(orderId))
                .thenReturn(List.of(existingSucceeded));

        PaymentCallbackRequest request = new PaymentCallbackRequest(
                orderId, "GW-REF-RETRY", "SUCCESS", new BigDecimal("100000.00"));

        PaymentTransactionDto response = paymentService.handleCallback(request, validSecret);

        assertThat(response.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(response.getProviderReference()).isEqualTo("GW-REF-RETRY");

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(orderClient);
        verifyNoInteractions(outboxService);
        verifyNoInteractions(exceptionClient);
    }

    @Test
    @DisplayName("Step 4 & BR-002: Duplicate SUCCESS callback for already paid order logs exception and rejects")
    void testHandleCallback_DuplicateSuccess_ViolatesBR002() {
        UUID orderId = UUID.randomUUID();
        PaymentTransaction existingSucceeded = new PaymentTransaction(orderId, new BigDecimal("100000.00"));
        existingSucceeded.markSucceeded("GW-REF-FIRST");

        when(paymentRepository.findByOrderIdOrderByAttemptedAtDesc(orderId))
                .thenReturn(List.of(existingSucceeded));
        when(orderClient.getOrderStatus(orderId)).thenReturn("PAID");

        PaymentCallbackRequest duplicateRequest = new PaymentCallbackRequest(
                orderId, "GW-REF-SECOND", "SUCCESS", new BigDecimal("100000.00"));

        assertThatThrownBy(() -> paymentService.handleCallback(duplicateRequest, validSecret))
                .isInstanceOf(BusinessRuleException.class)
                .matches(ex -> ((BusinessRuleException) ex).getRuleId().equals("BR-002"));

        verify(exceptionClient).createExceptionRecord(
                eq("DUPLICATE_PAYMENT"),
                eq(orderId.toString()),
                eq("ORDER"),
                eq("BR-002"),
                anyString(),
                anyString()
        );
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Step 5: Late callback for EXPIRED or CANCELLED order logs exception and rejects")
    void testHandleCallback_LateCallback_ExpiredOrder() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderIdOrderByAttemptedAtDesc(orderId))
                .thenReturn(Collections.emptyList());
        when(orderClient.getOrderStatus(orderId)).thenReturn("EXPIRED");

        PaymentCallbackRequest request = new PaymentCallbackRequest(
                orderId, "GW-REF-LATE", "SUCCESS", new BigDecimal("100000.00"));

        assertThatThrownBy(() -> paymentService.handleCallback(request, validSecret))
                .isInstanceOf(BusinessRuleException.class)
                .matches(ex -> ((BusinessRuleException) ex).getRuleId().equals("BR-001"));

        verify(exceptionClient).createExceptionRecord(
                eq("LATE_OR_STALE_PAYMENT_CALLBACK"),
                eq(orderId.toString()),
                eq("ORDER"),
                eq("LATE_PAYMENT_CALLBACK"),
                anyString(),
                anyString()
        );
        verify(orderClient, never()).updateOrderStatus(any(), eq("PAID"), any());
    }

    @Test
    @DisplayName("Manual reconciliation: succeeds with evidence and reason (FR-039, BR-012)")
    void testManualReconcile_Success() {
        UUID txId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentTransaction failedTx = new PaymentTransaction(orderId, new BigDecimal("300000.00"));
        failedTx.setId(txId);
        failedTx.markFailed("GW-OLD");

        when(paymentRepository.findById(txId)).thenReturn(Optional.of(failedTx));
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        ManualReconcileRequest request = new ManualReconcileRequest("EVID-BANK-SLIP-999", "Customer verified transfer");

        PaymentTransactionDto response = paymentService.reconcilePayment(txId, request);

        assertThat(response.getStatus()).isEqualTo("SUCCEEDED");
        assertThat(response.getEvidenceReference()).isEqualTo("EVID-BANK-SLIP-999");
        assertThat(response.getReconciliationReason()).isEqualTo("Customer verified transfer");

        verify(outboxService).recordEvent(eq(txId.toString()), eq("PaymentSucceeded"), any(PaymentSucceededEvent.class));
        verify(orderClient).updateOrderStatus(eq(orderId), eq("PAID"), contains("Customer verified transfer"));
    }

    @Test
    @DisplayName("Manual reconciliation: missing evidence or reason throws BusinessRuleException BR-012")
    void testManualReconcile_MissingEvidence_ThrowsBR012() {
        UUID txId = UUID.randomUUID();
        ManualReconcileRequest request = new ManualReconcileRequest("", "Some reason");

        assertThatThrownBy(() -> paymentService.reconcilePayment(txId, request))
                .isInstanceOf(BusinessRuleException.class)
                .matches(ex -> ((BusinessRuleException) ex).getRuleId().equals("BR-012"));

        verifyNoInteractions(paymentRepository);
        verifyNoInteractions(outboxService);
    }

    @Test
    @DisplayName("Manual reconciliation: non-existent transaction throws NotFoundException")
    void testManualReconcile_NotFound() {
        UUID txId = UUID.randomUUID();
        when(paymentRepository.findById(txId)).thenReturn(Optional.empty());

        ManualReconcileRequest request = new ManualReconcileRequest("EVID-123", "Reason");

        assertThatThrownBy(() -> paymentService.reconcilePayment(txId, request))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("checkTimeouts: transitions INITIATED transactions older than threshold to TIMEOUT")
    void testCheckTimeouts() {
        UUID orderId = UUID.randomUUID();
        PaymentTransaction timedOutTx = new PaymentTransaction(orderId, new BigDecimal("500000.00"));

        when(paymentRepository.findTimedOutTransactions(eq(PaymentStatus.INITIATED), any(Instant.class)))
                .thenReturn(List.of(timedOutTx));
        when(paymentRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        int processed = paymentService.checkTimeouts(15);

        assertThat(processed).isEqualTo(1);
        assertThat(timedOutTx.getStatus()).isEqualTo(PaymentStatus.TIMEOUT);
        verify(outboxService).recordEvent(anyString(), eq("PaymentTimeout"), any(PaymentTimeoutEvent.class));
        verify(inventoryClient).releaseStock(eq(orderId), contains("timeout"));
        verify(orderClient).updateOrderStatus(eq(orderId), eq("EXPIRED"), contains("timeout"));
    }

    @Test
    @DisplayName("getAnomalies: queries transactions with FAILED or TIMEOUT status")
    void testGetAnomalies() {
        PaymentTransaction failedTx = new PaymentTransaction(UUID.randomUUID(), new BigDecimal("100000.00"));
        failedTx.setStatus(PaymentStatus.FAILED);
        Page<PaymentTransaction> page = new PageImpl<>(List.of(failedTx));

        when(paymentRepository.findByStatusIn(anyCollection(), any(PageRequest.class)))
                .thenReturn(page);

        Page<PaymentTransactionDto> result = paymentService.getAnomalies(null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("FAILED");
    }
}
