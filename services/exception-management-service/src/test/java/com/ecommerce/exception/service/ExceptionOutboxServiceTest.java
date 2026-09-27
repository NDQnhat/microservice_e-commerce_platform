package com.ecommerce.exception.service;

import com.ecommerce.common.outbox.OutboxStatus;
import com.ecommerce.exception.domain.model.ExceptionRecord;
import com.ecommerce.exception.domain.model.ExceptionType;
import com.ecommerce.exception.domain.model.OutboxEventRecord;
import com.ecommerce.exception.domain.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExceptionOutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ExceptionOutboxService outboxService;

    private ExceptionRecord resolvedRecord;

    @BeforeEach
    void setUp() {
        outboxService = new ExceptionOutboxService(
                outboxEventRepository,
                objectMapper,
                kafkaTemplateProvider,
                "exception-events"
        );

        resolvedRecord = ExceptionRecord.create(
                ExceptionType.PAYMENT_FAILED,
                "payment-service",
                "ORD-555",
                "ORDER",
                "GATEWAY_ERR",
                "Charge failed",
                null
        );
        resolvedRecord.resolve("ADMIN_OPERATOR", "MANUAL_RETRY", "Bank cleared the dispute");
    }

    @Test
    @DisplayName("recordExceptionResolved: serializes payload and saves pending outbox event")
    void recordExceptionResolved_savesPendingEvent() {
        when(outboxEventRepository.save(any(OutboxEventRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        OutboxEventRecord record = outboxService.recordExceptionResolved(resolvedRecord);

        assertThat(record).isNotNull();
        assertThat(record.getAggregateType()).isEqualTo("ExceptionRecord");
        assertThat(record.getAggregateId()).isEqualTo(resolvedRecord.getId().toString());
        assertThat(record.getEventType()).isEqualTo("ExceptionResolved");
        assertThat(record.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(record.getPayload()).contains("\"exception_id\":\"" + resolvedRecord.getId() + "\"");
        assertThat(record.getPayload()).contains("\"resolved_by\":\"ADMIN_OPERATOR\"");
        assertThat(record.getPayload()).contains("\"resolution_action\":\"MANUAL_RETRY\"");
        assertThat(record.getPayload()).contains("\"resolution_notes\":\"Bank cleared the dispute\"");

        verify(outboxEventRepository).save(any(OutboxEventRecord.class));
    }

    @Test
    @DisplayName("recordExceptionIgnored: serializes payload and saves pending outbox event")
    void recordExceptionIgnored_savesPendingEvent() {
        ExceptionRecord ignoredRecord = ExceptionRecord.create(
                ExceptionType.NOTIFICATION_FAILED,
                "notification-service",
                "NOTI-111",
                "NOTIFICATION",
                "DISPATCH_ERR",
                "Failed to send email",
                null
        );
        ignoredRecord.ignore("ADMIN_OPERATOR", "User unsubscribed");

        when(outboxEventRepository.save(any(OutboxEventRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        OutboxEventRecord record = outboxService.recordExceptionIgnored(ignoredRecord);

        assertThat(record).isNotNull();
        assertThat(record.getEventType()).isEqualTo("ExceptionIgnored");
        assertThat(record.getPayload()).contains("\"resolution_notes\":\"User unsubscribed\"");
        verify(outboxEventRepository).save(any(OutboxEventRecord.class));
    }

    @Test
    @DisplayName("publishPendingEvents: sends message via KafkaTemplate and marks SENT")
    void publishPendingEvents_success() {
        OutboxEventRecord pendingRecord = OutboxEventRecord.createPending(
                "ExceptionRecord",
                resolvedRecord.getId().toString(),
                "ExceptionResolved",
                "{\"sample\":\"data\"}"
        );

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(pendingRecord));
        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        when(kafkaTemplate.send(eq("exception-events"), eq(pendingRecord.getAggregateId()), eq(pendingRecord.getPayload())))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxService.publishPendingEvents();

        assertThat(pendingRecord.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(pendingRecord.getProcessedAt()).isNotNull();
        verify(outboxEventRepository).save(pendingRecord);
    }

    @Test
    @DisplayName("publishPendingEvents: increments retry and marks FAILED after 5 retries")
    void publishPendingEvents_failure() {
        OutboxEventRecord pendingRecord = OutboxEventRecord.createPending(
                "ExceptionRecord",
                resolvedRecord.getId().toString(),
                "ExceptionResolved",
                "{\"sample\":\"data\"}"
        );
        pendingRecord.setRetryCount(4); // 4 existing retries, 5th should fail

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(pendingRecord));
        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        when(kafkaTemplate.send(any(), any(), any()))
                .thenThrow(new RuntimeException("Kafka broker unreachable"));

        outboxService.publishPendingEvents();

        assertThat(pendingRecord.getRetryCount()).isEqualTo(5);
        assertThat(pendingRecord.getStatus()).isEqualTo(OutboxStatus.FAILED);
        verify(outboxEventRepository).save(pendingRecord);
    }
}
