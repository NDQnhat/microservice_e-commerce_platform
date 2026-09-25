package com.ecommerce.inventory.service;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.common.event.EventEnvelope;
import com.ecommerce.inventory.domain.model.OutboxEventRecord;
import com.ecommerce.inventory.domain.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryOutboxService {

    private static final Logger log = LoggerFactory.getLogger(InventoryOutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public InventoryOutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public <T> void recordEvent(String aggregateType, String aggregateId, String eventType, T payload) {
        try {
            String correlationId = CorrelationContext.getCorrelationId();
            EventEnvelope<T> envelope = new EventEnvelope<>(
                    eventType,
                    aggregateType,
                    aggregateId,
                    correlationId,
                    payload
            );
            String jsonPayload = objectMapper.writeValueAsString(envelope);

            OutboxEventRecord record = new OutboxEventRecord(
                    aggregateType,
                    aggregateId,
                    eventType,
                    jsonPayload,
                    correlationId
            );
            outboxEventRepository.save(record);
            log.info("Recorded outbox event: type={}, aggregateId={}", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload: type={}, aggregateId={}", eventType, aggregateId, e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}
