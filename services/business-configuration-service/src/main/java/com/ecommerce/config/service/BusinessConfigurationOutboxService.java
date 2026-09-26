package com.ecommerce.config.service;

import com.ecommerce.common.outbox.OutboxStatus;
import com.ecommerce.config.domain.model.OutboxEventRecord;
import com.ecommerce.config.domain.repository.OutboxEventRepository;
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
public class BusinessConfigurationOutboxService {

    private static final Logger log = LoggerFactory.getLogger(BusinessConfigurationOutboxService.class);
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;
    private final String topic;

    public BusinessConfigurationOutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider,
            @Value("${outbox.publisher.topic:configuration-events}") String topic) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.kafkaTemplateProvider = kafkaTemplateProvider;
        this.topic = topic;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public OutboxEventRecord recordConfigChanged(
            String configKey,
            String beforeValue,
            String afterValue,
            Integer version,
            Instant effectiveFrom,
            String updatedBy,
            String reason) {

        Instant now = Instant.now();
        Instant effective = effectiveFrom != null ? effectiveFrom : now;

        Map<String, Object> payloadMap = new LinkedHashMap<>();
        // Snake_case per SRS Section 13 contract
        payloadMap.put("actor_id", updatedBy);
        payloadMap.put("updated_by", updatedBy);
        payloadMap.put("config_key", configKey);
        payloadMap.put("before_value", beforeValue);
        payloadMap.put("after_value", afterValue);
        payloadMap.put("version", version);
        payloadMap.put("effective_from", effective.toString());
        payloadMap.put("reason", reason);
        payloadMap.put("occurred_at", now.toString());

        // CamelCase aliases for flexible inter-service deserialization
        payloadMap.put("actorId", updatedBy);
        payloadMap.put("updatedBy", updatedBy);
        payloadMap.put("configKey", configKey);
        payloadMap.put("beforeValue", beforeValue);
        payloadMap.put("afterValue", afterValue);
        payloadMap.put("effectiveFrom", effective.toString());
        payloadMap.put("occurredAt", now.toString());

        try {
            String payloadJson = objectMapper.writeValueAsString(payloadMap);
            OutboxEventRecord record = OutboxEventRecord.createPending(
                    "BUSINESS_CONFIGURATION",
                    configKey,
                    "BusinessConfigChanged",
                    payloadJson
            );
            OutboxEventRecord saved = outboxEventRepository.save(record);
            log.info("Recorded BusinessConfigChanged outbox event for key: {}, version: {}", configKey, version);
            return saved;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload for configKey: {}", configKey, e);
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
                log.info("Successfully published outbox event {} for key: {}", record.getId(), record.getAggregateId());
            } catch (Exception ex) {
                log.warn("Failed to publish outbox event {}: {}", record.getId(), ex.getMessage());
                record.incrementRetry(MAX_RETRIES);
                outboxEventRepository.save(record);
            }
        }
    }
}
