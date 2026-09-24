package com.ecommerce.notification.api.dto;

import java.time.Instant;
import java.util.UUID;

public class NotificationLogDto {

    private UUID id;
    private UUID orderId;
    private UUID customerId;
    private UUID templateId;
    private String channel;
    private String status;
    private int attemptCount;
    private Instant lastAttemptAt;

    public NotificationLogDto() {
    }

    public NotificationLogDto(UUID id, UUID orderId, UUID customerId, UUID templateId,
                              String channel, String status, int attemptCount, Instant lastAttemptAt) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.templateId = templateId;
        this.channel = channel;
        this.status = status;
        this.attemptCount = attemptCount;
        this.lastAttemptAt = lastAttemptAt;
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

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(Instant lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }
}
