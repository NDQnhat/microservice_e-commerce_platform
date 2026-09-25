package com.ecommerce.identity.outbox;

import com.ecommerce.common.outbox.OutboxStatus;
import com.ecommerce.identity.domain.model.OutboxEventRecord;
import com.ecommerce.identity.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(outboxEventRepository, kafkaTemplateProvider);
    }

    @Test
    @DisplayName("Transactional Outbox: Dispatched event updates status to SENT with timestamp")
    void processRecord_Success_UpdatesStatusToSent() {
        OutboxEventRecord record = new OutboxEventRecord(
                "USER", UUID.randomUUID().toString(), "RoleAssigned", "{\"action\":\"ASSIGN\"}", "corr-1"
        );

        when(kafkaTemplate.send(eq("identity.events"), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        outboxPublisher.processRecord(record, kafkaTemplate);

        assertThat(record.getStatus()).isEqualTo(OutboxStatus.SENT);
        assertThat(record.getSentAt()).isNotNull();
        assertThat(record.getErrorMessage()).isNull();
        verify(outboxEventRepository).save(record);
    }

    @Test
    @DisplayName("Transactional Outbox: Failed dispatch increments retryCount")
    void processRecord_Failure_IncrementsRetryCount() {
        OutboxEventRecord record = new OutboxEventRecord(
                "USER", UUID.randomUUID().toString(), "RoleAssigned", "{\"action\":\"ASSIGN\"}", "corr-1"
        );

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka unreachable"));
        when(kafkaTemplate.send(eq("identity.events"), any(), any())).thenReturn(failedFuture);

        outboxPublisher.processRecord(record, kafkaTemplate);

        assertThat(record.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(record.getRetryCount()).isEqualTo(1);
        assertThat(record.getErrorMessage()).contains("Kafka unreachable");
        verify(outboxEventRepository).save(record);
    }

    @Test
    @DisplayName("Transactional Outbox: Max retries exceeded marks status as FAILED")
    void processRecord_MaxRetriesExceeded_MarksFailed() {
        OutboxEventRecord record = new OutboxEventRecord(
                "USER", UUID.randomUUID().toString(), "RoleAssigned", "{\"action\":\"ASSIGN\"}", "corr-1"
        );
        record.setRetryCount(4); // Next retry is 5 (MAX_RETRIES)

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka down"));
        when(kafkaTemplate.send(eq("identity.events"), any(), any())).thenReturn(failedFuture);

        outboxPublisher.processRecord(record, kafkaTemplate);

        assertThat(record.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(record.getRetryCount()).isEqualTo(5);
        verify(outboxEventRepository).save(record);
    }
}
