package com.ecommerce.audit.service.dto;

import com.ecommerce.audit.domain.model.AuditActionType;

import java.time.Instant;
import java.util.UUID;

public record AuditLogFilter(
        UUID actorId,
        String actorRole,
        AuditActionType actionType,
        String entityType,
        String entityId,
        Instant from,
        Instant to
) {
    public static AuditLogFilter of(
            UUID actorId,
            String actorRole,
            AuditActionType actionType,
            String entityType,
            String entityId,
            Instant from,
            Instant to
    ) {
        return new AuditLogFilter(actorId, actorRole, actionType, entityType, entityId, from, to);
    }
}
