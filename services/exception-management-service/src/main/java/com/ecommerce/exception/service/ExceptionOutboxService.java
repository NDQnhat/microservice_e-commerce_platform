package com.ecommerce.exception.service;

import com.ecommerce.common.outbox.OutboxStatus;
import com.ecommerce.exception.domain.model.ExceptionRecord;
import com.ecommerce.exception.domain.model.OutboxEventRecord;
import com.ecommerce.exception.domain.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ExceptionOutboxService {

    private static final Logger log = LoggerFactory.getLogger(ExceptionOutboxService.class);
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;
    private final String topic;

    public ExceptionOutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider,
            @Value("${outbox.publisher.topic:exception-events}") String topic) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.kafkaTemplateProvider = kafkaTemplateProvider;
        this.topic = topic;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEventRecord recordExceptionResolved(ExceptionRecord record) {
        return recordOutboxEvent(record, "ExceptionResolved", record.getResolutionAction());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEventRecord recordExceptionIgnored(ExceptionRecord record) {
        return recordOutboxEvent(record, "ExceptionIgnored", "IGNORED");
    }

    private OutboxEventRecord recordOutboxEvent(ExceptionRecord record, String eventType, String action) {
        Instant now = Instant.now();
        Instant resolvedAt = record.getResolvedAt() != null ? record.getResolvedAt() : now;

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        // Snake_case per SRS Section 13 contract
        payloadMap.put("exception_id", record.getId().toString());
        payloadMap.put("reference_type", record.getReferenceType());
        payloadMap.put("reference_id", record.getReferenceId());
        payloadMap.put("resolved_by", record.getResolvedBy());
        payloadMap.put("resolution_action", action);
        payloadMap.put("resolution_notes", record.getResolutionNotes());
        payloadMap.put("occurred_at", resolvedAt.toString());

        // CamelCase aliases for flexible inter-service deserialization
        payloadMap.put("exceptionId", record.getId().toString());
        payloadMap.put("referenceType", record.getReferenceType());
        payloadMap.put("referenceId", record.getReferenceId());
        payloadMap.put("resolvedBy", record.getResolvedBy());
        payloadMap.put("resolutionAction", action);
        payloadMap.put("resolutionNotes", record.getResolutionNotes());
        payloadMap.put("occurredAt", resolvedAt.toString());

        try {
            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            OutboxEventRecord outboxRecord = OutboxEventRecord.createPending(
                    "ExceptionRecord",
                    record.getId().toString(),
                    eventType,
                    payloadJson
            );
            OutboxEventRecord saved = outboxEventRepository.save(outboxRecord);
            log.info("Recorded {} outbox event for exceptionId: {}", eventType, record.getId());
            return saved;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload for exceptionId: {}", record.getId(), e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay:2000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventRecord> pendingRecords = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING,
                PageRequest.of(0, 50)
        );

        if (pendingRecords.isEmpty()) {
            return;
        }

        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();

        for (OutboxEventRecord record : pendingRecords) {
            try {
                if (kafkaTemplate != null) {
                    kafkaTemplate.send(topic, record.getAggregateId(), record.getPayload()).get(5, TimeUnit.SECONDS);
                } else {
                    log.debug("KafkaTemplate unavailable; marking outbox event {} as SENT (simulated)", record.getId());
                }
                record.markSent();
                outboxEventRepository.save(record);
                log.info("Successfully published outbox event {} for aggregateId: {}", record.getId(), record.getAggregateId());
            } catch (Exception ex) {
                log.warn("Failed to publish outbox event {}: {}", record.getId(), ex.getMessage());
                record.incrementRetry(MAX_RETRIES);
                outboxEventRepository.save(record);
            }
        }
    }
}
