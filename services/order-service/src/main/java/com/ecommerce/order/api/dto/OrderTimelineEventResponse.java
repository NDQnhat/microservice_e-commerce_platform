package com.ecommerce.order.api.dto;

import java.time.Instant;
import java.util.UUID;

public class OrderTimelineEventResponse {

    private UUID id;
    private UUID orderId;
    private String fromStatus;
    private String toStatus;
    private UUID actorId;
    private String actorType;
    private String note;
    private Instant occurredAt;

    public OrderTimelineEventResponse() {
    }

    public OrderTimelineEventResponse(UUID id, UUID orderId, String fromStatus,
                                      String toStatus, UUID actorId, String actorType,
                                      String note, Instant occurredAt) {
        this.id = id;
        this.orderId = orderId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actorId = actorId;
        this.actorType = actorType;
        this.note = note;
        this.occurredAt = occurredAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
