package com.ecommerce.exception.event;

import com.ecommerce.exception.api.dto.CreateExceptionRecordRequest;
import com.ecommerce.exception.api.dto.ExceptionRecordDto;
import com.ecommerce.exception.domain.model.ExceptionType;
import com.ecommerce.exception.service.ExceptionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExceptionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExceptionEventConsumer.class);

    private final ExceptionService exceptionService;
    private final ObjectMapper objectMapper;

    public ExceptionEventConsumer(ExceptionService exceptionService, ObjectMapper objectMapper) {
        this.exceptionService = exceptionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Kafka listener for domain events across services.
     * Configured with auto-startup flag to avoid blocking test suites when Kafka is offline.
     */
    @KafkaListener(
            topics = {"payment-events", "notification-events", "order-events"},
            groupId = "exception-management-group",
            autoStartup = "${spring.kafka.consumer.auto-startup:false}"
    )
    public void onKafkaMessage(Object rawMessage) {
        if (rawMessage == null) {
            return;
        }

        try {
            Map<String, Object> messageMap = convertToMap(rawMessage);
            if (messageMap == null || messageMap.isEmpty()) {
                return;
            }

            // Extract event type (supports both flat and envelope style)
            String eventType = extractString(messageMap, "eventType", "event_type", "type");
            Map<String, Object> payload = messageMap;

            Object innerPayload = messageMap.get("payload");
            if (innerPayload != null) {
                Map<String, Object> convertedInner = convertToMap(innerPayload);
                if (convertedInner != null) {
                    payload = convertedInner;
                }
            }

            if (eventType != null) {
                consumeEvent(eventType, payload);
            }
        } catch (Exception ex) {
            log.error("Failed to process Kafka exception event: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Core event processing and mapping engine.
     * Maps cross-service domain anomalies to ExceptionRecord entries.
     */
    public ExceptionRecordDto consumeEvent(String eventType, Map<String, Object> payload) {
        if (eventType == null || payload == null) {
            return null;
        }

        try {
            CreateExceptionRecordRequest request = mapEventToExceptionRequest(eventType, payload);
            if (request != null) {
                return exceptionService.recordException(request);
            }
        } catch (Exception ex) {
            log.warn("Failed to ingest exception event of type [{}]: {}", eventType, ex.getMessage());
        }
        return null;
    }

    private CreateExceptionRecordRequest mapEventToExceptionRequest(String eventType, Map<String, Object> payload) {
        String normalizedType = eventType.toUpperCase();

        // 1. Payment Duplicate Callback (BR-002 Step 4)
        if (normalizedType.contains("DUPLICATE") || isFlagTrue(payload, "duplicate", "is_duplicate", "isDuplicate")) {
            return mapDuplicatePayment(payload);
        }

        // 2. Late or Stale Payment Callback (Step 5)
        if (normalizedType.contains("LATE") || normalizedType.contains("STALE")
                || isFlagTrue(payload, "late_callback", "is_late", "isLate", "isStale")) {
            return mapLatePaymentCallback(payload);
        }

        // 3. Payment Failed (PAY-T03)
        if (normalizedType.contains("PAYMENTFAILED") || normalizedType.contains("PAYMENT_FAILED")) {
            return mapPaymentFailed(payload);
        }

        // 4. Notification Delivery Failed (Section 13)
        if (normalizedType.contains("NOTIFICATION") && (normalizedType.contains("FAILED") || normalizedType.contains("FAILURE"))) {
            return mapNotificationFailed(payload);
        }

        // 5. Stuck Order (ORD-T04 / Pipeline stuck)
        if (normalizedType.contains("STUCK") || normalizedType.contains("ORDER_TIMEOUT") || normalizedType.contains("ORDERTIMEOUT")) {
            return mapStuckOrder(payload);
        }

        // 6. Inventory Discrepancy
        if (normalizedType.contains("INVENTORY") && (normalizedType.contains("DISCREPANCY") || normalizedType.contains("MISMATCH"))) {
            return mapInventoryDiscrepancy(payload);
        }

        // 7. General / Unknown Error
        if (normalizedType.contains("ERROR") || normalizedType.contains("EXCEPTION")) {
            return mapUnknownProcessingError(eventType, payload);
        }

        log.debug("Event type [{}] does not generate an exception record", eventType);
        return null;
    }

    private CreateExceptionRecordRequest mapDuplicatePayment(Map<String, Object> payload) {
        String orderId = extractString(payload, "orderId", "order_id", "referenceId", "reference_id");
        String txnId = extractString(payload, "transactionId", "transaction_id", "paymentId", "payment_id");
        String refId = orderId != null ? orderId : (txnId != null ? txnId : "UNKNOWN_ORDER");
        String message = extractString(payload, "message", "reason", "errorMessage", "error_message");
        if (message == null) {
            message = "Duplicate payment callback received for already processed order";
        }

        return new CreateExceptionRecordRequest(
                ExceptionType.DUPLICATE_PAYMENT,
                "payment-service",
                refId,
                "ORDER",
                "DUPLICATE_PAYMENT_CALLBACK",
                message,
                serializePayloadSafe(payload)
        );
    }

    private CreateExceptionRecordRequest mapLatePaymentCallback(Map<String, Object> payload) {
        String orderId = extractString(payload, "orderId", "order_id", "referenceId", "reference_id");
        String txnId = extractString(payload, "transactionId", "transaction_id");
        String refId = orderId != null ? orderId : (txnId != null ? txnId : "UNKNOWN_ORDER");
        String message = extractString(payload, "message", "reason", "errorMessage", "error_message");
        if (message == null) {
            message = "Payment callback arrived after order was cancelled or expired";
        }

        return new CreateExceptionRecordRequest(
                ExceptionType.LATE_OR_STALE_PAYMENT_CALLBACK,
                "payment-service",
                refId,
                "ORDER",
                "LATE_PAYMENT_CALLBACK",
                message,
                serializePayloadSafe(payload)
        );
    }

    private CreateExceptionRecordRequest mapPaymentFailed(Map<String, Object> payload) {
        String orderId = extractString(payload, "orderId", "order_id");
        String txnId = extractString(payload, "transactionId", "transaction_id");
        String refId = orderId != null ? orderId : (txnId != null ? txnId : "UNKNOWN_PAYMENT");
        String message = extractString(payload, "errorMessage", "error_message", "reason", "message");
        if (message == null) {
            message = "Payment transaction failed during payment gateway processing";
        }
        String errorCode = extractString(payload, "errorCode", "error_code");
        if (errorCode == null) {
            errorCode = "PAYMENT_FAILED";
        }

        return new CreateExceptionRecordRequest(
                ExceptionType.PAYMENT_FAILED,
                "payment-service",
                refId,
                "ORDER",
                errorCode,
                message,
                serializePayloadSafe(payload)
        );
    }

    private CreateExceptionRecordRequest mapNotificationFailed(Map<String, Object> payload) {
        String notiId = extractString(payload, "notificationLogId", "notification_log_id", "notificationId", "notification_id", "id");
        String refId = notiId != null ? notiId : "UNKNOWN_NOTIFICATION";
        String reason = extractString(payload, "reason", "errorMessage", "error_message", "message");
        if (reason == null) {
            reason = "Notification dispatch attempt failed";
        }

        return new CreateExceptionRecordRequest(
                ExceptionType.NOTIFICATION_FAILED,
                "notification-service",
                refId,
                "NOTIFICATION",
                "NOTIFICATION_DELIVERY_FAILED",
                reason,
                serializePayloadSafe(payload)
        );
    }

    private CreateExceptionRecordRequest mapStuckOrder(Map<String, Object> payload) {
        String orderId = extractString(payload, "orderId", "order_id", "referenceId", "reference_id", "id");
        String refId = orderId != null ? orderId : "UNKNOWN_ORDER";
        String reason = extractString(payload, "reason", "errorMessage", "error_message", "message");
        if (reason == null) {
            reason = "Order stuck in intermediate state without progression";
        }

        return new CreateExceptionRecordRequest(
                ExceptionType.STUCK_ORDER,
                "order-service",
                refId,
                "ORDER",
                "ORDER_STUCK",
                reason,
                serializePayloadSafe(payload)
        );
    }

    private CreateExceptionRecordRequest mapInventoryDiscrepancy(Map<String, Object> payload) {
        String skuId = extractString(payload, "skuId", "sku_id", "referenceId", "reference_id");
        String refId = skuId != null ? skuId : "UNKNOWN_SKU";
        String reason = extractString(payload, "reason", "errorMessage", "error_message", "message");
        if (reason == null) {
            reason = "Inventory discrepancy detected during stock reconciliation";
        }

        return new CreateExceptionRecordRequest(
                ExceptionType.INVENTORY_DISCREPANCY,
                "inventory-service",
                refId,
                "SKU",
                "INVENTORY_DISCREPANCY",
                reason,
                serializePayloadSafe(payload)
        );
    }

    private CreateExceptionRecordRequest mapUnknownProcessingError(String eventType, Map<String, Object> payload) {
        String refId = extractString(payload, "referenceId", "reference_id", "id", "orderId", "order_id");
        if (refId == null) {
            refId = "UNKNOWN_ENTITY";
        }
        String refType = extractString(payload, "referenceType", "reference_type");
        if (refType == null) {
            refType = "SYSTEM";
        }
        String message = extractString(payload, "errorMessage", "error_message", "reason", "message");
        if (message == null) {
            message = "Unspecified processing error in event: " + eventType;
        }

        return new CreateExceptionRecordRequest(
                ExceptionType.UNKNOWN_PROCESSING_ERROR,
                "system",
                refId,
                refType,
                "PROCESSING_ERROR",
                message,
                serializePayloadSafe(payload)
        );
    }

    private boolean isFlagTrue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object val = payload.get(key);
            if (val instanceof Boolean b && b) {
                return true;
            }
            if (val instanceof String s && ("true".equalsIgnoreCase(s) || "1".equals(s))) {
                return true;
            }
        }
        return false;
    }

    private String serializePayloadSafe(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ignored) {
            return payload.toString();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> convertToMap(Object obj) {
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        if (obj instanceof String str) {
            try {
                return objectMapper.readValue(str, new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
                return null;
            }
        }
        try {
            return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractString(Map<String, Object> payload, String... keys) {
        if (payload == null) return null;
        for (String key : keys) {
            Object val = payload.get(key);
            if (val != null) {
                String str = val.toString().trim();
                if (!str.isEmpty()) {
                    return str;
                }
            }
        }
        return null;
    }
}
