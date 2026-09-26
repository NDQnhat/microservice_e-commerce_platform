package com.ecommerce.notification.event;

import com.ecommerce.notification.api.dto.DispatchNotificationRequest;
import com.ecommerce.notification.api.dto.DispatchNotificationResponse;
import com.ecommerce.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class NotificationEventHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventHandler.class);

    private final NotificationService notificationService;

    public NotificationEventHandler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Generic event handler that maps domain event type and payload to notification dispatch.
     * Guaranteed never to throw exceptions (Fault Isolation - FR-013, NFR-FAULTISO-001).
     */
    public DispatchNotificationResponse handleDomainEvent(String eventType, Map<String, Object> payload) {
        try {
            log.info("Handling domain event: [{}] for notification dispatch", eventType);

            String eventCode = mapEventTypeToEventCode(eventType);
            UUID orderId = extractUuid(payload, "order_id", "orderId");
            UUID customerId = extractUuid(payload, "customer_id", "customerId");
            String recipient = extractString(payload, "recipient", "email", "customer_email");

            Map<String, Object> params = new HashMap<>();
            if (payload != null) {
                params.putAll(payload);
            }

            DispatchNotificationRequest request = new DispatchNotificationRequest(
                    eventCode,
                    "EMAIL",
                    orderId,
                    customerId,
                    recipient,
                    params,
                    false
            );

            return notificationService.dispatchNotification(request);
        } catch (Exception ex) {
            log.error("Error processing domain event [{}] in notification handler: {}", eventType, ex.getMessage(), ex);
            return new DispatchNotificationResponse(
                    null,
                    "FAILED",
                    "EMAIL",
                    eventType,
                    null,
                    null,
                    "Event processing failure: " + ex.getMessage()
            );
        }
    }

    /**
     * Optional Kafka listener enabled when Kafka consumer auto-startup is true.
     */
    @KafkaListener(
            topics = {"order-events", "payment-events", "fulfillment-events"},
            groupId = "notification-service-group",
            autoStartup = "${spring.kafka.consumer.auto-startup:false}"
    )
    public void onKafkaMessage(Map<String, Object> message) {
        if (message == null) {
            return;
        }
        String eventType = (String) message.getOrDefault("event_type", message.get("eventType"));
        if (eventType != null) {
            handleDomainEvent(eventType, message);
        }
    }

    public String mapEventTypeToEventCode(String eventType) {
        if (eventType == null) {
            return "UNKNOWN";
        }
        // Normalize names: e.g. OrderCreated -> ORDER_CREATED, OrderStatusChanged -> ORDER_STATUS_CHANGED
        return eventType
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase();
    }

    private UUID extractUuid(Map<String, Object> payload, String... keys) {
        if (payload == null) return null;
        for (String key : keys) {
            Object val = payload.get(key);
            if (val instanceof UUID uuid) {
                return uuid;
            }
            if (val instanceof String s && !s.isBlank()) {
                try {
                    return UUID.fromString(s);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return null;
    }

    private String extractString(Map<String, Object> payload, String... keys) {
        if (payload == null) return null;
        for (String key : keys) {
            Object val = payload.get(key);
            if (val != null) {
                return val.toString();
            }
        }
        return null;
    }
}
