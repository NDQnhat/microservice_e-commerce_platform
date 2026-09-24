package com.ecommerce.common.event;

import java.time.Instant;
import java.util.UUID;

public class EventEnvelope<T> {

    private String eventId;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String correlationId;
    private Instant timestamp;
    private T payload;

    public EventEnvelope() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    public EventEnvelope(String eventType, String aggregateType, String aggregateId,
                         String correlationId, T payload) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.correlationId = correlationId;
        this.timestamp = Instant.now();
        this.payload = payload;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
