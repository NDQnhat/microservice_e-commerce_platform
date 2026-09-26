package com.ecommerce.audit.event;

import com.ecommerce.audit.api.dto.AuditLogDto;
import com.ecommerce.audit.api.dto.RecordAuditRequest;
import com.ecommerce.audit.domain.model.AuditActionType;
import com.ecommerce.audit.service.AuditService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);
    private static final UUID SYSTEM_ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditEventConsumer(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Kafka listener for domain events across services.
     * Configured with auto-startup flag to avoid blocking test suites when Kafka is offline.
     */
    @KafkaListener(
            topics = {"identity-events", "configuration-events", "inventory-events", "order-events", "payment-events"},
            groupId = "audit-compliance-group",
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
            log.error("Failed to process Kafka audit event: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Core event processing and mapping engine.
     * Maps domain events to immutable AuditLog records.
     */
    public AuditLogDto consumeEvent(String eventType, Map<String, Object> payload) {
        if (eventType == null || payload == null) {
            return null;
        }

        try {
            RecordAuditRequest request = mapEventToAuditRequest(eventType, payload);
            if (request != null) {
                return auditService.recordAuditLog(request);
            }
        } catch (Exception ex) {
            log.warn("Failed to ingest audit event of type [{}]: {}", eventType, ex.getMessage());
        }
        return null;
    }

    private RecordAuditRequest mapEventToAuditRequest(String eventType, Map<String, Object> payload) {
        String normalizedType = eventType.toUpperCase();

        if (normalizedType.contains("ROLEASSIGNED") || normalizedType.contains("ROLE_ASSIGN")) {
            return mapRoleAssigned(payload);
        } else if (normalizedType.contains("CONFIG") || normalizedType.contains("CONFIGURATION")) {
            return mapConfigChanged(payload);
        } else if (normalizedType.contains("INVENTORYADJUST") || normalizedType.contains("INVENTORY_ADJUST")) {
            return mapInventoryAdjusted(payload);
        } else if (normalizedType.contains("ORDERSTATUSCHANGED") || normalizedType.contains("ORDER_STATUS")
                || normalizedType.contains("ORDERCANCELLED") || normalizedType.contains("ORDER_CANCEL")) {
            return mapOrderStatusChanged(payload);
        } else if (normalizedType.contains("PAYMENTRECONCIL") || normalizedType.contains("PAYMENT_RECONCIL")
                || (normalizedType.contains("PAYMENTSUCCEEDED") && isReconciled(payload))) {
            return mapPaymentReconciled(payload);
        }

        log.debug("Event type [{}] does not require compliance audit recording", eventType);
        return null;
    }

    private RecordAuditRequest mapRoleAssigned(Map<String, Object> payload) {
        UUID actorId = extractUuid(payload, "actorId", "actor_id");
        if (actorId == null) {
            actorId = SYSTEM_ACTOR_ID;
        }
        String actorRole = extractString(payload, "actorRole", "actor_role");
        if (actorRole == null) {
            actorRole = "SUPER_ADMIN";
        }

        String targetUserId = extractString(payload, "targetUserId", "target_user_id", "userId", "user_id");
        String roleCode = extractString(payload, "roleCode", "role_code");
        String action = extractString(payload, "action", "action_type");
        if (action == null) {
            action = "ASSIGN";
        }

        return new RecordAuditRequest(
                actorId,
                actorRole,
                AuditActionType.ROLE_ASSIGN,
                "USER",
                targetUserId != null ? targetUserId : "UNKNOWN",
                null,
                roleCode,
                action + " role " + roleCode
        );
    }

    private RecordAuditRequest mapConfigChanged(Map<String, Object> payload) {
        UUID actorId = extractUuid(payload, "actorId", "actor_id", "updatedBy", "updated_by");
        if (actorId == null) {
            actorId = SYSTEM_ACTOR_ID;
        }
        String actorRole = extractString(payload, "actorRole", "actor_role");
        if (actorRole == null) {
            actorRole = "SUPER_ADMIN";
        }

        String configKey = extractString(payload, "configKey", "config_key");
        String beforeValue = extractString(payload, "beforeValue", "before_value");
        String afterValue = extractString(payload, "afterValue", "after_value", "configValue", "config_value");
        String reason = extractString(payload, "reason", "description");
        if (reason == null) {
            reason = "Dynamic business configuration updated";
        }

        return new RecordAuditRequest(
                actorId,
                actorRole,
                AuditActionType.CONFIG_CHANGE,
                "BUSINESS_CONFIGURATION",
                configKey != null ? configKey : "GLOBAL_CONFIG",
                beforeValue,
                afterValue,
                reason
        );
    }

    private RecordAuditRequest mapInventoryAdjusted(Map<String, Object> payload) {
        UUID actorId = extractUuid(payload, "actorId", "actor_id");
        if (actorId == null) {
            actorId = SYSTEM_ACTOR_ID;
        }
        String actorRole = extractString(payload, "actorRole", "actor_role");
        if (actorRole == null) {
            actorRole = "INVENTORY_MANAGER";
        }

        String skuId = extractString(payload, "skuId", "sku_id");
        String qtyBefore = extractString(payload, "quantityBefore", "quantity_before");
        String qtyAfter = extractString(payload, "quantityAfter", "quantity_after");
        String delta = extractString(payload, "delta");
        String reasonCode = extractString(payload, "reasonCode", "reason_code", "note");
        if (reasonCode == null) {
            reasonCode = "CORRECTION";
        }

        return new RecordAuditRequest(
                actorId,
                actorRole,
                AuditActionType.INVENTORY_ADJUST,
                "INVENTORY",
                skuId != null ? skuId : "UNKNOWN_SKU",
                qtyBefore,
                qtyAfter != null ? qtyAfter : ("delta=" + delta),
                reasonCode
        );
    }

    private RecordAuditRequest mapOrderStatusChanged(Map<String, Object> payload) {
        UUID actorId = extractUuid(payload, "actorId", "actor_id", "cancelled_by_actor_id");
        if (actorId == null) {
            actorId = SYSTEM_ACTOR_ID;
        }
        String actorRole = extractString(payload, "actorRole", "actor_role", "actorType", "actor_type");
        if (actorRole == null) {
            actorRole = "ORDER_OPERATIONS_ADMIN";
        }

        String orderId = extractString(payload, "orderId", "order_id", "id");
        String fromStatus = extractString(payload, "fromStatus", "from_status");
        String toStatus = extractString(payload, "toStatus", "to_status", "status");
        String note = extractString(payload, "note", "reason");
        if (note == null) {
            note = "Order state transition to " + toStatus;
        }

        return new RecordAuditRequest(
                actorId,
                actorRole,
                AuditActionType.ORDER_STATE_TRANSITION,
                "ORDER",
                orderId != null ? orderId : "UNKNOWN_ORDER",
                fromStatus,
                toStatus,
                note
        );
    }

    private RecordAuditRequest mapPaymentReconciled(Map<String, Object> payload) {
        UUID actorId = extractUuid(payload, "actorId", "actor_id", "operatorId", "operator_id");
        if (actorId == null) {
            actorId = SYSTEM_ACTOR_ID;
        }
        String actorRole = extractString(payload, "actorRole", "actor_role");
        if (actorRole == null) {
            actorRole = "ORDER_OPERATIONS_ADMIN";
        }

        String transactionId = extractString(payload, "transactionId", "transaction_id", "id");
        String orderId = extractString(payload, "orderId", "order_id");
        String entityId = transactionId != null ? transactionId : (orderId != null ? orderId : "UNKNOWN_TXN");
        String beforeStatus = extractString(payload, "previousStatus", "previous_status", "fromStatus", "from_status");
        String reason = extractString(payload, "reason", "evidenceReference", "evidence_reference");
        if (reason == null || reason.isBlank()) {
            reason = "Manual reconciliation approved per BR-012";
        }

        return new RecordAuditRequest(
                actorId,
                actorRole,
                AuditActionType.PAYMENT_RECONCILE,
                "PAYMENT_TRANSACTION",
                entityId,
                beforeStatus != null ? beforeStatus : "TIMEOUT",
                "SUCCEEDED",
                reason
        );
    }

    private boolean isReconciled(Map<String, Object> payload) {
        Object rec = payload.get("reconciled");
        if (rec instanceof Boolean b) {
            return b;
        }
        if (rec instanceof String s) {
            return Boolean.parseBoolean(s);
        }
        return false;
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

    private UUID extractUuid(Map<String, Object> payload, String... keys) {
        if (payload == null) return null;
        for (String key : keys) {
            Object val = payload.get(key);
            if (val instanceof UUID u) return u;
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
