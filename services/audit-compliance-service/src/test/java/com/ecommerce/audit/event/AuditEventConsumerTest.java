package com.ecommerce.audit.event;

import com.ecommerce.audit.api.dto.AuditLogDto;
import com.ecommerce.audit.api.dto.RecordAuditRequest;
import com.ecommerce.audit.domain.model.AuditActionType;
import com.ecommerce.audit.service.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditEventConsumerTest {

    @Mock
    private AuditService auditService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AuditEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuditEventConsumer(auditService, objectMapper);
    }

    @Test
    @DisplayName("AuditEventConsumer: processes RoleAssigned event to ROLE_ASSIGN audit record")
    void shouldConsumeRoleAssigned() {
        UUID actorId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "actor_id", actorId.toString(),
                "actor_role", "SUPER_ADMIN",
                "target_user_id", targetUserId.toString(),
                "role_code", "ROLE_ADMIN",
                "action", "ASSIGN"
        );

        when(auditService.recordAuditLog(any(RecordAuditRequest.class))).thenReturn(new AuditLogDto());

        AuditLogDto result = consumer.consumeEvent("RoleAssigned", payload);

        assertThat(result).isNotNull();
        ArgumentCaptor<RecordAuditRequest> captor = ArgumentCaptor.forClass(RecordAuditRequest.class);
        verify(auditService).recordAuditLog(captor.capture());

        RecordAuditRequest req = captor.getValue();
        assertThat(req.getActorId()).isEqualTo(actorId);
        assertThat(req.getActionType()).isEqualTo(AuditActionType.ROLE_ASSIGN);
        assertThat(req.getEntityType()).isEqualTo("USER");
        assertThat(req.getEntityId()).isEqualTo(targetUserId.toString());
        assertThat(req.getAfterValue()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("AuditEventConsumer: processes BusinessConfigChanged event to CONFIG_CHANGE audit record")
    void shouldConsumeBusinessConfigChanged() {
        UUID actorId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "actor_id", actorId.toString(),
                "config_key", "RESERVATION_TTL_MINUTES",
                "before_value", "15",
                "after_value", "30",
                "reason", "Holiday adjustment"
        );

        when(auditService.recordAuditLog(any(RecordAuditRequest.class))).thenReturn(new AuditLogDto());

        AuditLogDto result = consumer.consumeEvent("BusinessConfigChanged", payload);

        assertThat(result).isNotNull();
        ArgumentCaptor<RecordAuditRequest> captor = ArgumentCaptor.forClass(RecordAuditRequest.class);
        verify(auditService).recordAuditLog(captor.capture());

        RecordAuditRequest req = captor.getValue();
        assertThat(req.getActionType()).isEqualTo(AuditActionType.CONFIG_CHANGE);
        assertThat(req.getEntityType()).isEqualTo("BUSINESS_CONFIGURATION");
        assertThat(req.getEntityId()).isEqualTo("RESERVATION_TTL_MINUTES");
        assertThat(req.getBeforeValue()).isEqualTo("15");
        assertThat(req.getAfterValue()).isEqualTo("30");
        assertThat(req.getReason()).isEqualTo("Holiday adjustment");
    }

    @Test
    @DisplayName("AuditEventConsumer: processes InventoryAdjusted event to INVENTORY_ADJUST audit record")
    void shouldConsumeInventoryAdjusted() {
        UUID actorId = UUID.randomUUID();
        UUID skuId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "actor_id", actorId.toString(),
                "sku_id", skuId.toString(),
                "quantity_before", "10",
                "quantity_after", "25",
                "reason_code", "RESTOCK"
        );

        when(auditService.recordAuditLog(any(RecordAuditRequest.class))).thenReturn(new AuditLogDto());

        AuditLogDto result = consumer.consumeEvent("InventoryAdjusted", payload);

        assertThat(result).isNotNull();
        ArgumentCaptor<RecordAuditRequest> captor = ArgumentCaptor.forClass(RecordAuditRequest.class);
        verify(auditService).recordAuditLog(captor.capture());

        RecordAuditRequest req = captor.getValue();
        assertThat(req.getActionType()).isEqualTo(AuditActionType.INVENTORY_ADJUST);
        assertThat(req.getEntityType()).isEqualTo("INVENTORY");
        assertThat(req.getEntityId()).isEqualTo(skuId.toString());
        assertThat(req.getBeforeValue()).isEqualTo("10");
        assertThat(req.getAfterValue()).isEqualTo("25");
        assertThat(req.getReason()).isEqualTo("RESTOCK");
    }

    @Test
    @DisplayName("AuditEventConsumer: processes OrderStatusChanged event to ORDER_STATE_TRANSITION audit record")
    void shouldConsumeOrderStatusChanged() {
        UUID actorId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "actor_id", actorId.toString(),
                "order_id", orderId.toString(),
                "from_status", "RESERVED",
                "to_status", "PAID",
                "note", "Payment confirmed"
        );

        when(auditService.recordAuditLog(any(RecordAuditRequest.class))).thenReturn(new AuditLogDto());

        AuditLogDto result = consumer.consumeEvent("OrderStatusChanged", payload);

        assertThat(result).isNotNull();
        ArgumentCaptor<RecordAuditRequest> captor = ArgumentCaptor.forClass(RecordAuditRequest.class);
        verify(auditService).recordAuditLog(captor.capture());

        RecordAuditRequest req = captor.getValue();
        assertThat(req.getActionType()).isEqualTo(AuditActionType.ORDER_STATE_TRANSITION);
        assertThat(req.getEntityType()).isEqualTo("ORDER");
        assertThat(req.getEntityId()).isEqualTo(orderId.toString());
        assertThat(req.getBeforeValue()).isEqualTo("RESERVED");
        assertThat(req.getAfterValue()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("AuditEventConsumer: processes PaymentReconciled event to PAYMENT_RECONCILE audit record")
    void shouldConsumePaymentReconciled() {
        UUID actorId = UUID.randomUUID();
        UUID txnId = UUID.randomUUID();
        Map<String, Object> payload = Map.of(
                "actor_id", actorId.toString(),
                "transaction_id", txnId.toString(),
                "previous_status", "TIMEOUT",
                "reason", "Bank receipt ref 9999"
        );

        when(auditService.recordAuditLog(any(RecordAuditRequest.class))).thenReturn(new AuditLogDto());

        AuditLogDto result = consumer.consumeEvent("PaymentReconciled", payload);

        assertThat(result).isNotNull();
        ArgumentCaptor<RecordAuditRequest> captor = ArgumentCaptor.forClass(RecordAuditRequest.class);
        verify(auditService).recordAuditLog(captor.capture());

        RecordAuditRequest req = captor.getValue();
        assertThat(req.getActionType()).isEqualTo(AuditActionType.PAYMENT_RECONCILE);
        assertThat(req.getEntityType()).isEqualTo("PAYMENT_TRANSACTION");
        assertThat(req.getEntityId()).isEqualTo(txnId.toString());
        assertThat(req.getReason()).isEqualTo("Bank receipt ref 9999");
    }

    @Test
    @DisplayName("AuditEventConsumer: unwraps EventEnvelope message correctly")
    void shouldUnwrapEventEnvelope() {
        UUID actorId = UUID.randomUUID();
        Map<String, Object> innerPayload = Map.of(
                "actor_id", actorId.toString(),
                "config_key", "CANCELLATION_WINDOW_MINUTES",
                "before_value", "30",
                "after_value", "60",
                "reason", "Policy update"
        );

        Map<String, Object> envelope = Map.of(
                "eventType", "BusinessConfigChanged",
                "aggregateType", "CONFIGURATION",
                "payload", innerPayload
        );

        when(auditService.recordAuditLog(any(RecordAuditRequest.class))).thenReturn(new AuditLogDto());

        consumer.onKafkaMessage(envelope);

        verify(auditService).recordAuditLog(any(RecordAuditRequest.class));
    }

    @Test
    @DisplayName("AuditEventConsumer: ignores unknown event types gracefully without error")
    void shouldIgnoreUnknownEventTypes() {
        AuditLogDto result = consumer.consumeEvent("UnrelatedEvent", Map.of("key", "val"));
        assertThat(result).isNull();
        verify(auditService, never()).recordAuditLog(any());
    }
}
