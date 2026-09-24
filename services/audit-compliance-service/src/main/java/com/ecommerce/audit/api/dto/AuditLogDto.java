package com.ecommerce.audit.api.dto;

import java.time.Instant;
import java.util.UUID;

public class AuditLogDto {

    private UUID id;
    private UUID actorId;
    private String actorRole;
    private String actionType;
    private String entityType;
    private String entityId;
    private String beforeValue;
    private String afterValue;
    private String reason;
    private Instant createdAt;

    public AuditLogDto() {
    }

    public AuditLogDto(UUID id, UUID actorId, String actorRole, String actionType,
                       String entityType, String entityId, String beforeValue,
                       String afterValue, String reason, Instant createdAt) {
        this.id = id;
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getActorRole() {
        return actorRole;
    }

    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(String beforeValue) {
        this.beforeValue = beforeValue;
    }

    public String getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(String afterValue) {
        this.afterValue = afterValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
