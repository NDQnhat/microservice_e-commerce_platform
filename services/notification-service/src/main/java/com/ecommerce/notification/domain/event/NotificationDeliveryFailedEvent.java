package com.ecommerce.notification.domain.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record NotificationDeliveryFailedEvent(
        @JsonProperty("notification_log_id") UUID notificationLogId,
        @JsonProperty("reason") String reason,
        @JsonProperty("occurred_at") Instant occurredAt
) {
    public NotificationDeliveryFailedEvent(UUID notificationLogId, String reason) {
        this(notificationLogId, reason, Instant.now());
    }
}
