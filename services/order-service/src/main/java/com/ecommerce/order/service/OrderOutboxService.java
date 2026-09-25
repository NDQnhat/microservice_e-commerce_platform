package com.ecommerce.order.service;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.order.domain.model.OutboxEventRecord;
import com.ecommerce.order.domain.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderOutboxService {

    private static final Logger log = LoggerFactory.getLogger(OrderOutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OrderOutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(String aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String correlationId = CorrelationContext.getCorrelationId();

            OutboxEventRecord record = new OutboxEventRecord(
                    "Order",
                    aggregateId,
                    eventType,
                    payloadJson,
                    correlationId
            );
            outboxEventRepository.save(record);
            log.debug("Recorded outbox event: {} for aggregate: {}", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}
