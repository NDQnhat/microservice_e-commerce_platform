package com.ecommerce.config.service;

import com.ecommerce.common.outbox.OutboxStatus;
import com.ecommerce.config.domain.model.OutboxEventRecord;
import com.ecommerce.config.domain.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessConfigurationOutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BusinessConfigurationOutboxService outboxService;

    @BeforeEach
    void setUp() {
        outboxService = new BusinessConfigurationOutboxService(
                outboxEventRepository,
                objectMapper,
                kafkaTemplateProvider,
                "configuration-events"
        );
    }

    @Test
    @DisplayName("recordConfigChanged: persists outbox record with PENDING status and valid SRS Section 13 payload")
    void shouldRecordConfigChanged() throws Exception {
        when(outboxEventRepository.save(any(OutboxEventRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant effectiveFrom = Instant.now();
        OutboxEventRecord record = outboxService.recordConfigChanged(
                "ORDER_CANCELLATION_TIMEOUT_MINUTES",
                "15",
                "20",
                2,
                effectiveFrom,
                "SUPER_ADMIN_ACTOR",
                "Policy update"
        );

        assertThat(record).isNotNull();
        assertThat(record.getAggregateType()).isEqualTo("BUSINESS_CONFIGURATION");
        assertThat(record.getAggregateId()).isEqualTo("ORDER_CANCELLATION_TIMEOUT_MINUTES");
        assertThat(record.getEventType()).isEqualTo("BusinessConfigChanged");
        assertThat(record.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(record.getRetryCount()).isEqualTo(0);

        JsonNode payload = objectMapper.readTree(record.getPayload());
        assertThat(payload.get("actor_id").asText()).isEqualTo("SUPER_ADMIN_ACTOR");
        assertThat(payload.get("config_key").asText()).isEqualTo("ORDER_CANCELLATION_TIMEOUT_MINUTES");
        assertThat(payload.get("before_value").asText()).isEqualTo("15");
        assertThat(payload.get("after_value").asText()).isEqualTo("20");
        assertThat(payload.get("version").asInt()).isEqualTo(2);
        assertThat(payload.get("reason").asText()).isEqualTo("Policy update");
        assertThat(payload.has("occurred_at")).isTrue();
        assertThat(payload.has("effective_from")).isTrue();

        // Also check camelCase aliases for interoperability
        assertThat(payload.get("actorId").asText()).isEqualTo("SUPER_ADMIN_ACTOR");
        assertThat(payload.get("configKey").asText()).isEqualTo("ORDER_CANCELLATION_TIMEOUT_MINUTES");
    }

    @Test
    @DisplayName("publishPendingEvents: publishes events to Kafka and marks status SENT")
    void shouldPublishPendingEvents() {
        OutboxEventRecord record = OutboxEventRecord.createPending(
                "BUSINESS_CONFIGURATION",
                "KEY1",
                "BusinessConfigChanged",
                "{\"config_key\":\"KEY1\"}"
        );

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(record));
        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        when(kafkaTemplate.send(eq("configuration-events"), eq("KEY1"), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        outboxService.publishPendingEvents();

        assertThat(record.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(record.getProcessedAt()).isNotNull();
        verify(outboxEventRepository).save(record);
    }

    @Test
    @DisplayName("publishPendingEvents: marks status SENT even when KafkaTemplate is not configured (simulated)")
    void shouldPublishWhenKafkaNotAvailable() {
        OutboxEventRecord record = OutboxEventRecord.createPending(
                "BUSINESS_CONFIGURATION",
                "KEY1",
                "BusinessConfigChanged",
                "{\"config_key\":\"KEY1\"}"
        );

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(record));
        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(null);

        outboxService.publishPendingEvents();

        assertThat(record.getStatus()).isEqualTo(OutboxStatus.SENT);
        verify(outboxEventRepository).save(record);
    }

    @Test
    @DisplayName("publishPendingEvents: increments retry on failure and transitions to FAILED when limit exceeded")
    void shouldHandlePublishFailureAndMarkFailed() {
        OutboxEventRecord record = OutboxEventRecord.createPending(
                "BUSINESS_CONFIGURATION",
                "KEY1",
                "BusinessConfigChanged",
                "{\"config_key\":\"KEY1\"}"
        );
        record.setRetryCount(4); // 4th failure, next should hit 5 (MAX_RETRIES)

        when(outboxEventRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(record));
        when(kafkaTemplateProvider.getIfAvailable()).thenReturn(kafkaTemplate);
        when(kafkaTemplate.send(any(), any(), any()))
                .thenThrow(new RuntimeException("Kafka cluster unreachable"));

        outboxService.publishPendingEvents();

        assertThat(record.getRetryCount()).isEqualTo(5);
        assertThat(record.getStatus()).isEqualTo(OutboxStatus.FAILED);
        verify(outboxEventRepository).save(record);
    }
}
