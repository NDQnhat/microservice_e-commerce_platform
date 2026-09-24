package com.ecommerce.notification.domain;

import com.ecommerce.notification.domain.model.NotificationDeliveryStatus;
import com.ecommerce.notification.domain.model.NotificationLog;
import com.ecommerce.notification.domain.model.NotificationTemplate;
import com.ecommerce.notification.domain.model.TemplateStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDomainTest {

    @Test
    @DisplayName("Notification template defaults to ACTIVE status")
    void templateShouldDefaultToActive() {
        NotificationTemplate template = new NotificationTemplate("ORDER_CREATED", "EMAIL", "Order Confirmation", "Hello {{name}}");

        assertThat(template.getId()).isNotNull();
        assertThat(template.getStatus()).isEqualTo(TemplateStatus.ACTIVE);
        assertThat(template.getEventCode()).isEqualTo("ORDER_CREATED");
    }

    @Test
    @DisplayName("Notification log initializes with attempt count 1")
    void logShouldInitializeAttemptCount() {
        UUID orderId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        NotificationLog log = new NotificationLog(orderId, null, templateId, "EMAIL", NotificationDeliveryStatus.SENT);

        assertThat(log.getId()).isNotNull();
        assertThat(log.getAttemptCount()).isEqualTo(1);
        assertThat(log.getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
    }
}
