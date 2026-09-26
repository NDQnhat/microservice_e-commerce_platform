package com.ecommerce.notification.service;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.notification.domain.model.OutboxEventRecord;
import com.ecommerce.notification.domain.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationOutboxService {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public NotificationOutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(String aggregateType, String aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String correlationId = CorrelationContext.getCorrelationId();

            OutboxEventRecord record = new OutboxEventRecord(
                    aggregateType,
                    aggregateId,
                    eventType,
                    payloadJson,
                    correlationId
            );
            outboxEventRepository.save(record);
            log.debug("Recorded outbox event: {} for aggregate: {}/{}", eventType, aggregateType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}
