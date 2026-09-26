package com.ecommerce.notification.service;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.notification.api.dto.*;
import com.ecommerce.notification.domain.event.NotificationDeliveryFailedEvent;
import com.ecommerce.notification.domain.model.NotificationDeliveryStatus;
import com.ecommerce.notification.domain.model.NotificationLog;
import com.ecommerce.notification.domain.model.NotificationTemplate;
import com.ecommerce.notification.domain.model.TemplateStatus;
import com.ecommerce.notification.domain.repository.NotificationLogRepository;
import com.ecommerce.notification.domain.repository.NotificationTemplateRepository;
import com.ecommerce.notification.engine.ChannelDispatcher;
import com.ecommerce.notification.engine.SendResult;
import com.ecommerce.notification.engine.TemplateRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private TemplateRenderer templateRenderer;

    @Mock
    private ChannelDispatcher channelDispatcher;

    @Mock
    private NotificationOutboxService outboxService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID templateId;
    private UUID logId;
    private UUID orderId;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        logId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
    }

    // =========================================================================
    // 1. Template Management (FR-035)
    // =========================================================================

    @Test
    @DisplayName("createTemplate: saves and returns template with ACTIVE status")
    void createTemplate_success() {
        CreateTemplateRequest request = new CreateTemplateRequest(
                "ORDER_CREATED", "EMAIL", "Order Confirmation", "Thank you {{name}}");

        when(templateRepository.save(any(NotificationTemplate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NotificationTemplateDto result = notificationService.createTemplate(request);

        assertThat(result).isNotNull();
        assertThat(result.getEventCode()).isEqualTo("ORDER_CREATED");
        assertThat(result.getChannel()).isEqualTo("EMAIL");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("updateTemplate: updates subject, bodyTemplate and status (Immutability for past logs - FR-035)")
    void updateTemplate_success() {
        NotificationTemplate existing = new NotificationTemplate(
                "ORDER_CREATED", "EMAIL", "Old Subject", "Old Body");
        existing.setId(templateId);

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(existing));
        when(templateRepository.save(any(NotificationTemplate.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "New Subject", "New Body {{order_id}}", TemplateStatus.INACTIVE);

        NotificationTemplateDto result = notificationService.updateTemplate(templateId, request);

        assertThat(result.getSubject()).isEqualTo("New Subject");
        assertThat(result.getBodyTemplate()).isEqualTo("New Body {{order_id}}");
        assertThat(result.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("updateTemplate: throws NotFoundException when template does not exist")
    void updateTemplate_notFound() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());

        UpdateTemplateRequest request = new UpdateTemplateRequest(
                "New Subject", "New Body", TemplateStatus.ACTIVE);

        assertThatThrownBy(() -> notificationService.updateTemplate(templateId, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(templateId.toString());
    }

    @Test
    @DisplayName("getTemplates: returns paginated list of templates with filters")
    void getTemplates_returnsPage() {
        NotificationTemplate t1 = new NotificationTemplate("ORDER_CREATED", "EMAIL", "Subj1", "Body1");
        Page<NotificationTemplate> page = new PageImpl<>(List.of(t1));

        when(templateRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<NotificationTemplateDto> result = notificationService.getTemplates(
                "ORDER_CREATED", "EMAIL", TemplateStatus.ACTIVE, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEventCode()).isEqualTo("ORDER_CREATED");
    }

    @Test
    @DisplayName("getTemplate: returns template detail by ID")
    void getTemplate_success() {
        NotificationTemplate template = new NotificationTemplate("ORDER_SHIPPED", "SMS", "Shipped", "Body");
        template.setId(templateId);

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        NotificationTemplateDto result = notificationService.getTemplate(templateId);

        assertThat(result.getId()).isEqualTo(templateId);
        assertThat(result.getEventCode()).isEqualTo("ORDER_SHIPPED");
    }

    // =========================================================================
    // 2. Delivery Log & Manual Retry (FR-013, FR-036)
    // =========================================================================

    @Test
    @DisplayName("getLogs: queries logs with filters and pagination")
    void getLogs_returnsPage() {
        NotificationLog logRecord = new NotificationLog(
                orderId, customerId, templateId, "EMAIL", NotificationDeliveryStatus.SENT);
        Page<NotificationLog> page = new PageImpl<>(List.of(logRecord));

        when(logRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<NotificationLogDto> result = notificationService.getLogs(
                NotificationDeliveryStatus.SENT, orderId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderId()).isEqualTo(orderId);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("SENT");
    }

    @Test
    @DisplayName("retryNotification: successful retry increments attemptCount and updates status to SENT")
    void retryNotification_success() {
        NotificationLog logRecord = new NotificationLog(
                orderId, customerId, templateId, "EMAIL", NotificationDeliveryStatus.FAILED);
        logRecord.setId(logId);

        NotificationTemplate template = new NotificationTemplate("ORDER_CREATED", "EMAIL", "Subj", "Body");
        template.setId(templateId);

        when(logRepository.findById(logId)).thenReturn(Optional.of(logRecord));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(channelDispatcher.dispatch(eq("EMAIL"), any(), eq("Subj"), eq("Body"), eq(false)))
                .thenReturn(SendResult.success("EMAIL", "test@test.com"));
        when(logRepository.save(any(NotificationLog.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationLogDto result = notificationService.retryNotification(logId);

        assertThat(result.getStatus()).isEqualTo("SENT");
        assertThat(result.getAttemptCount()).isEqualTo(2);
        verify(outboxService, never()).recordEvent(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("retryNotification: failed retry increments attemptCount, keeps FAILED, and emits outbox event")
    void retryNotification_failure() {
        NotificationLog logRecord = new NotificationLog(
                orderId, customerId, templateId, "EMAIL", NotificationDeliveryStatus.FAILED);
        logRecord.setId(logId);

        when(logRepository.findById(logId)).thenReturn(Optional.of(logRecord));
        when(templateRepository.findById(templateId)).thenReturn(Optional.empty());
        when(channelDispatcher.dispatch(any(), any(), any(), any(), eq(false)))
                .thenReturn(SendResult.failure("EMAIL", "test@test.com", "Channel unavailable"));
        when(logRepository.save(any(NotificationLog.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationLogDto result = notificationService.retryNotification(logId);

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getAttemptCount()).isEqualTo(2);
        verify(outboxService).recordEvent(
                eq("NotificationLog"),
                eq(logId.toString()),
                eq("NotificationDeliveryFailed"),
                any(NotificationDeliveryFailedEvent.class)
        );
    }

    // =========================================================================
    // 3. Dispatch Notification & Fault Isolation (FR-013, NFR-FAULTISO-001)
    // =========================================================================

    @Test
    @DisplayName("dispatchNotification: successfully renders and dispatches notification, saves SENT log")
    void dispatchNotification_success() {
        NotificationTemplate template = new NotificationTemplate(
                "ORDER_CREATED", "EMAIL", "Order {{order_id}}", "Hello {{name}}");
        template.setId(templateId);

        when(templateRepository.findByEventCodeAndChannelAndStatus("ORDER_CREATED", "EMAIL", TemplateStatus.ACTIVE))
                .thenReturn(Optional.of(template));
        when(templateRenderer.render(eq("Order {{order_id}}"), anyMap()))
                .thenReturn("Order 12345");
        when(templateRenderer.render(eq("Hello {{name}}"), anyMap()))
                .thenReturn("Hello Bob");
        when(channelDispatcher.dispatch(eq("EMAIL"), eq("bob@example.com"), eq("Order 12345"), eq("Hello Bob"), eq(false)))
                .thenReturn(SendResult.success("EMAIL", "bob@example.com"));

        when(logRepository.save(any(NotificationLog.class))).thenAnswer(inv -> inv.getArgument(0));

        DispatchNotificationRequest request = new DispatchNotificationRequest(
                "ORDER_CREATED", "EMAIL", orderId, customerId, "bob@example.com", Map.of("name", "Bob"), false);

        DispatchNotificationResponse response = notificationService.dispatchNotification(request);

        assertThat(response.getStatus()).isEqualTo("SENT");
        assertThat(response.getRenderedSubject()).isEqualTo("Order 12345");
        assertThat(response.getRenderedBody()).isEqualTo("Hello Bob");
        assertThat(response.getErrorMessage()).isNull();

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationDeliveryStatus.SENT);
        assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("dispatchNotification: channel failure isolates fault, saves FAILED log, emits outbox event (FR-013, FR-038)")
    void dispatchNotification_channelFailure_faultIsolation() {
        NotificationTemplate template = new NotificationTemplate(
                "ORDER_CREATED", "EMAIL", "Subject", "Body");
        template.setId(templateId);

        when(templateRepository.findByEventCodeAndChannelAndStatus("ORDER_CREATED", "EMAIL", TemplateStatus.ACTIVE))
                .thenReturn(Optional.of(template));
        when(templateRenderer.render(anyString(), anyMap())).thenReturn("Rendered");
        when(channelDispatcher.dispatch(eq("EMAIL"), any(), any(), any(), eq(true)))
                .thenReturn(SendResult.failure("EMAIL", "bob@example.com", "SMTP server unreachable"));

        when(logRepository.save(any(NotificationLog.class))).thenAnswer(inv -> inv.getArgument(0));

        DispatchNotificationRequest request = new DispatchNotificationRequest(
                "ORDER_CREATED", "EMAIL", orderId, customerId, "bob@example.com", Map.of(), true);

        // Crucial test: must NOT throw exception, preserves business pipeline fault isolation
        DispatchNotificationResponse response = notificationService.dispatchNotification(request);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getErrorMessage()).contains("SMTP server unreachable");

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationDeliveryStatus.FAILED);

        verify(outboxService).recordEvent(
                eq("NotificationLog"),
                anyString(),
                eq("NotificationDeliveryFailed"),
                any(NotificationDeliveryFailedEvent.class)
        );
    }

    @Test
    @DisplayName("dispatchNotification: no active template returns FAILED status gracefully without throwing")
    void dispatchNotification_noTemplate() {
        when(templateRepository.findByEventCodeAndChannelAndStatus("UNKNOWN_EVENT", "EMAIL", TemplateStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(templateRepository.findFirstByEventCodeAndStatus("UNKNOWN_EVENT", TemplateStatus.ACTIVE))
                .thenReturn(Optional.empty());

        DispatchNotificationRequest request = new DispatchNotificationRequest(
                "UNKNOWN_EVENT", "EMAIL", orderId, customerId, "bob@example.com", Map.of(), false);

        DispatchNotificationResponse response = notificationService.dispatchNotification(request);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getErrorMessage()).contains("No active template found");
        verify(channelDispatcher, never()).dispatch(any(), any(), any(), any(), anyBoolean());
    }
}
