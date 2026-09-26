package com.ecommerce.notification.service;

import com.ecommerce.notification.api.dto.*;
import com.ecommerce.notification.domain.model.NotificationDeliveryStatus;
import com.ecommerce.notification.domain.model.TemplateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {

    // Template management (FR-035)
    NotificationTemplateDto createTemplate(CreateTemplateRequest request);

    NotificationTemplateDto updateTemplate(UUID templateId, UpdateTemplateRequest request);

    Page<NotificationTemplateDto> getTemplates(String eventCode, String channel, TemplateStatus status, Pageable pageable);

    NotificationTemplateDto getTemplate(UUID templateId);

    // Delivery log & manual retry (FR-013, FR-036)
    Page<NotificationLogDto> getLogs(NotificationDeliveryStatus status, UUID orderId, Pageable pageable);

    NotificationLogDto getLog(UUID logId);

    NotificationLogDto retryNotification(UUID logId);

    // Dispatch (FR-013, NFR-FAULTISO-001)
    DispatchNotificationResponse dispatchNotification(DispatchNotificationRequest request);
}
