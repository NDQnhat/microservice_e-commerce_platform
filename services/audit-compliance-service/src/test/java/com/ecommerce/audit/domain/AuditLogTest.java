package com.ecommerce.audit.domain;

import com.ecommerce.audit.domain.model.AuditActionType;
import com.ecommerce.audit.domain.model.AuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditLogTest {

    @Test
    @DisplayName("AuditLog entity is instantiated with required fields and timestamp")
    void shouldCreateAuditLog() {
        UUID actorId = UUID.randomUUID();
        AuditLog log = new AuditLog(
                actorId,
                "SUPER_ADMIN",
                AuditActionType.CONFIG_CHANGE,
                "BusinessConfiguration",
                "RESERVATION_TTL_MINUTES",
                "15",
                "30",
                "Updated TTL for holiday season"
        );

        assertThat(log.getId()).isNotNull();
        assertThat(log.getActorId()).isEqualTo(actorId);
        assertThat(log.getActorRole()).isEqualTo("SUPER_ADMIN");
        assertThat(log.getActionType()).isEqualTo(AuditActionType.CONFIG_CHANGE);
        assertThat(log.getCreatedAt()).isNotNull();
    }
}
