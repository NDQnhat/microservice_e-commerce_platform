package com.ecommerce.audit.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Column(name = "actor_role", nullable = false, updatable = false)
    private String actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, updatable = false)
    private AuditActionType actionType;

    @Column(name = "entity_type", nullable = false, updatable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false, updatable = false)
    private String entityId;

    @Column(name = "before_value", columnDefinition = "text", updatable = false)
    private String beforeValue;

    @Column(name = "after_value", columnDefinition = "text", updatable = false)
    private String afterValue;

    @Column(columnDefinition = "text", updatable = false)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AuditLog() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public AuditLog(UUID actorId, String actorRole, AuditActionType actionType,
                    String entityType, String entityId, String beforeValue,
                    String afterValue, String reason) {
        this.id = UUID.randomUUID();
        this.actorId = actorId;
        this.actorRole = actorRole;
        this.actionType = actionType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorRole() {
        return actorRole;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getBeforeValue() {
        return beforeValue;
    }

    public String getAfterValue() {
        return afterValue;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
