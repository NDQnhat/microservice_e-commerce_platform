package com.ecommerce.exception.event;

import com.ecommerce.exception.api.dto.CreateExceptionRecordRequest;
import com.ecommerce.exception.api.dto.ExceptionRecordDto;
import com.ecommerce.exception.domain.model.ExceptionRecordStatus;
import com.ecommerce.exception.domain.model.ExceptionType;
import com.ecommerce.exception.service.ExceptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExceptionEventConsumerTest {

    @Mock
    private ExceptionService exceptionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ExceptionEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new ExceptionEventConsumer(exceptionService, objectMapper);
    }

    @Test
    @DisplayName("consumeEvent: consumes PaymentFailed event and creates PAYMENT_FAILED record")
    void consumeEvent_paymentFailed() {
        Map<String, Object> payload = Map.of(
                "orderId", "ORD-1001",
                "transactionId", "TXN-2002",
                "errorCode", "PAY_DECLINED",
                "errorMessage", "Insufficient card balance"
        );

        when(exceptionService.recordException(any(CreateExceptionRecordRequest.class)))
                .thenAnswer(inv -> toDummyDto(inv.getArgument(0)));

        ExceptionRecordDto dto = consumer.consumeEvent("PaymentFailed", payload);

        assertThat(dto).isNotNull();
        ArgumentCaptor<CreateExceptionRecordRequest> captor = ArgumentCaptor.forClass(CreateExceptionRecordRequest.class);
        verify(exceptionService).recordException(captor.capture());

        CreateExceptionRecordRequest req = captor.getValue();
        assertThat(req.exceptionType()).isEqualTo(ExceptionType.PAYMENT_FAILED);
        assertThat(req.sourceService()).isEqualTo("payment-service");
        assertThat(req.referenceId()).isEqualTo("ORD-1001");
        assertThat(req.referenceType()).isEqualTo("ORDER");
        assertThat(req.errorCode()).isEqualTo("PAY_DECLINED");
    }

    @Test
    @DisplayName("consumeEvent: consumes duplicate payment callback and creates DUPLICATE_PAYMENT record (BR-002)")
    void consumeEvent_duplicatePayment() {
        Map<String, Object> payload = Map.of(
                "orderId", "ORD-8888",
                "transactionId", "TXN-9999",
                "is_duplicate", true,
                "message", "Payment callback received twice"
        );

        when(exceptionService.recordException(any(CreateExceptionRecordRequest.class)))
                .thenAnswer(inv -> toDummyDto(inv.getArgument(0)));

        ExceptionRecordDto dto = consumer.consumeEvent("DUPLICATE_PAYMENT", payload);

        assertThat(dto).isNotNull();
        ArgumentCaptor<CreateExceptionRecordRequest> captor = ArgumentCaptor.forClass(CreateExceptionRecordRequest.class);
        verify(exceptionService).recordException(captor.capture());

        CreateExceptionRecordRequest req = captor.getValue();
        assertThat(req.exceptionType()).isEqualTo(ExceptionType.DUPLICATE_PAYMENT);
        assertThat(req.sourceService()).isEqualTo("payment-service");
        assertThat(req.referenceId()).isEqualTo("ORD-8888");
        assertThat(req.errorCode()).isEqualTo("DUPLICATE_PAYMENT_CALLBACK");
    }

    @Test
    @DisplayName("consumeEvent: consumes late payment callback and creates LATE_OR_STALE_PAYMENT_CALLBACK record")
    void consumeEvent_latePaymentCallback() {
        Map<String, Object> payload = Map.of(
                "orderId", "ORD-7777",
                "is_late", true,
                "message", "Payment callback arrived after order expiration"
        );

        when(exceptionService.recordException(any(CreateExceptionRecordRequest.class)))
                .thenAnswer(inv -> toDummyDto(inv.getArgument(0)));

        ExceptionRecordDto dto = consumer.consumeEvent("LATE_OR_STALE_PAYMENT_CALLBACK", payload);

        assertThat(dto).isNotNull();
        ArgumentCaptor<CreateExceptionRecordRequest> captor = ArgumentCaptor.forClass(CreateExceptionRecordRequest.class);
        verify(exceptionService).recordException(captor.capture());

        CreateExceptionRecordRequest req = captor.getValue();
        assertThat(req.exceptionType()).isEqualTo(ExceptionType.LATE_OR_STALE_PAYMENT_CALLBACK);
        assertThat(req.referenceId()).isEqualTo("ORD-7777");
        assertThat(req.errorCode()).isEqualTo("LATE_PAYMENT_CALLBACK");
    }

    @Test
    @DisplayName("consumeEvent: consumes NotificationDeliveryFailed event and creates NOTIFICATION_FAILED record")
    void consumeEvent_notificationDeliveryFailed() {
        Map<String, Object> payload = Map.of(
                "notification_log_id", "NOTI-456",
                "reason", "SMTP server timeout",
                "occurred_at", Instant.now().toString()
        );

        when(exceptionService.recordException(any(CreateExceptionRecordRequest.class)))
                .thenAnswer(inv -> toDummyDto(inv.getArgument(0)));

        ExceptionRecordDto dto = consumer.consumeEvent("NotificationDeliveryFailed", payload);

        assertThat(dto).isNotNull();
        ArgumentCaptor<CreateExceptionRecordRequest> captor = ArgumentCaptor.forClass(CreateExceptionRecordRequest.class);
        verify(exceptionService).recordException(captor.capture());

        CreateExceptionRecordRequest req = captor.getValue();
        assertThat(req.exceptionType()).isEqualTo(ExceptionType.NOTIFICATION_FAILED);
        assertThat(req.sourceService()).isEqualTo("notification-service");
        assertThat(req.referenceId()).isEqualTo("NOTI-456");
        assertThat(req.referenceType()).isEqualTo("NOTIFICATION");
        assertThat(req.errorCode()).isEqualTo("NOTIFICATION_DELIVERY_FAILED");
    }

    @Test
    @DisplayName("consumeEvent: consumes OrderStuckDetected event and creates STUCK_ORDER record")
    void consumeEvent_orderStuck() {
        Map<String, Object> payload = Map.of(
                "order_id", "ORD-3333",
                "reason", "Order has been in RESERVED state exceeding threshold"
        );

        when(exceptionService.recordException(any(CreateExceptionRecordRequest.class)))
                .thenAnswer(inv -> toDummyDto(inv.getArgument(0)));

        ExceptionRecordDto dto = consumer.consumeEvent("OrderStuckDetected", payload);

        assertThat(dto).isNotNull();
        ArgumentCaptor<CreateExceptionRecordRequest> captor = ArgumentCaptor.forClass(CreateExceptionRecordRequest.class);
        verify(exceptionService).recordException(captor.capture());

        CreateExceptionRecordRequest req = captor.getValue();
        assertThat(req.exceptionType()).isEqualTo(ExceptionType.STUCK_ORDER);
        assertThat(req.sourceService()).isEqualTo("order-service");
        assertThat(req.referenceId()).isEqualTo("ORD-3333");
        assertThat(req.errorCode()).isEqualTo("ORDER_STUCK");
    }

    @Test
    @DisplayName("onKafkaMessage: parses json envelope message and triggers exception creation")
    void onKafkaMessage_envelopeMessage() {
        String envelopeJson = """
                {
                    "eventType": "PaymentFailed",
                    "payload": {
                        "orderId": "ORD-500",
                        "errorCode": "INSUFFICIENT_FUNDS",
                        "errorMessage": "Card declined"
                    }
                }
                """;

        when(exceptionService.recordException(any(CreateExceptionRecordRequest.class)))
                .thenAnswer(inv -> toDummyDto(inv.getArgument(0)));

        consumer.onKafkaMessage(envelopeJson);

        verify(exceptionService).recordException(any(CreateExceptionRecordRequest.class));
    }

    private ExceptionRecordDto toDummyDto(CreateExceptionRecordRequest req) {
        return new ExceptionRecordDto(
                UUID.randomUUID(),
                req.exceptionType(),
                req.sourceService(),
                req.referenceId(),
                req.referenceType(),
                req.errorCode(),
                req.errorMessage(),
                req.payload(),
                ExceptionRecordStatus.OPEN,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }
}
