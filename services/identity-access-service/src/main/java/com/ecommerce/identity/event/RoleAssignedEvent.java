package com.ecommerce.identity.event;

import java.time.Instant;
import java.util.UUID;

public class RoleAssignedEvent {

    private String actorId;
    private UUID targetUserId;
    private String roleCode;
    private String action; // "ASSIGN" or "REVOKE"
    private Instant occurredAt;

    public RoleAssignedEvent() {
    }

    public RoleAssignedEvent(String actorId, UUID targetUserId, String roleCode, String action) {
        this.actorId = actorId;
        this.targetUserId = targetUserId;
        this.roleCode = roleCode;
        this.action = action;
        this.occurredAt = Instant.now();
    }

    public RoleAssignedEvent(String actorId, UUID targetUserId, String roleCode, String action, Instant occurredAt) {
        this.actorId = actorId;
        this.targetUserId = targetUserId;
        this.roleCode = roleCode;
        this.action = action;
        this.occurredAt = occurredAt;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(UUID targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }
}
