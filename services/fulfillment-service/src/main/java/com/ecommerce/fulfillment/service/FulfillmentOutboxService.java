package com.ecommerce.fulfillment.service;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.fulfillment.domain.model.OutboxEventRecord;
import com.ecommerce.fulfillment.domain.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FulfillmentOutboxService {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentOutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public FulfillmentOutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(String aggregateId, String eventType, Object payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String correlationId = CorrelationContext.getCorrelationId();

            OutboxEventRecord record = new OutboxEventRecord(
                    "Shipment",
                    aggregateId,
                    eventType,
                    payloadJson,
                    correlationId
            );
            outboxEventRepository.save(record);
            log.debug("Recorded outbox event: {} for shipment: {}", eventType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
}
