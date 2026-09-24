package com.ecommerce.audit.api.dto;

import com.ecommerce.audit.domain.model.AuditActionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class RecordAuditRequest {

    @NotNull(message = "Actor ID is required")
    private UUID actorId;

    @NotBlank(message = "Actor role is required")
    private String actorRole;

    @NotNull(message = "Action type is required")
    private AuditActionType actionType;

    @NotBlank(message = "Entity type is required")
    private String entityType;

    @NotBlank(message = "Entity ID is required")
    private String entityId;

    private String beforeValue;
    private String afterValue;
    private String reason;

    public RecordAuditRequest() {
    }

    public RecordAuditRequest(UUID actorId, String actorRole, AuditActionType actionType,
                              String entityType, String entityId, String beforeValue,
                              String afterValue, String reason) {
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.reason = reason;
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

    public AuditActionType getActionType() {
        return actionType;
    }

    public void setActionType(AuditActionType actionType) {
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
}
