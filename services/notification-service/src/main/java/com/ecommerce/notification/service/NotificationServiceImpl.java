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
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final TemplateRenderer templateRenderer;
    private final ChannelDispatcher channelDispatcher;
    private final NotificationOutboxService outboxService;

    public NotificationServiceImpl(NotificationTemplateRepository templateRepository,
                                   NotificationLogRepository logRepository,
                                   TemplateRenderer templateRenderer,
                                   ChannelDispatcher channelDispatcher,
                                   NotificationOutboxService outboxService) {
        this.templateRepository = templateRepository;
        this.logRepository = logRepository;
        this.templateRenderer = templateRenderer;
        this.channelDispatcher = channelDispatcher;
        this.outboxService = outboxService;
    }

    // =========================================================================
    // 1. Template Management (FR-035)
    // =========================================================================

    @Override
    @Transactional
    public NotificationTemplateDto createTemplate(CreateTemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate(
                request.getEventCode(),
                request.getChannel(),
                request.getSubject(),
                request.getBodyTemplate()
        );
        NotificationTemplate saved = templateRepository.save(template);
        log.info("Created notification template ID: {} for event: {}", saved.getId(), saved.getEventCode());
        return toTemplateDto(saved);
    }

    @Override
    @Transactional
    public NotificationTemplateDto updateTemplate(UUID templateId, UpdateTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Notification template not found: " + templateId));

        template.update(request.getSubject(), request.getBodyTemplate(), request.getStatus());
        NotificationTemplate updated = templateRepository.save(template);
        log.info("Updated notification template ID: {}", updated.getId());
        return toTemplateDto(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationTemplateDto> getTemplates(String eventCode, String channel, TemplateStatus status, Pageable pageable) {
        Specification<NotificationTemplate> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (eventCode != null && !eventCode.isBlank()) {
                predicates.add(cb.equal(root.get("eventCode"), eventCode));
            }
            if (channel != null && !channel.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("channel")), channel.toUpperCase()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return templateRepository.findAll(spec, pageable).map(this::toTemplateDto);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateDto getTemplate(UUID templateId) {
        NotificationTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new NotFoundException("Notification template not found: " + templateId));
        return toTemplateDto(template);
    }

    // =========================================================================
    // 2. Delivery Log & Manual Retry (FR-013, FR-036)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationLogDto> getLogs(NotificationDeliveryStatus status, UUID orderId, Pageable pageable) {
        Specification<NotificationLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (orderId != null) {
                predicates.add(cb.equal(root.get("orderId"), orderId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return logRepository.findAll(spec, pageable).map(this::toLogDto);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationLogDto getLog(UUID logId) {
        NotificationLog notifLog = logRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("Notification log not found: " + logId));
        return toLogDto(notifLog);
    }

    @Override
    @Transactional
    public NotificationLogDto retryNotification(UUID logId) {
        NotificationLog notifLog = logRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("Notification log not found: " + logId));

        NotificationTemplate template = templateRepository.findById(notifLog.getTemplateId())
                .orElse(null);

        String subject = template != null ? template.getSubject() : "Notification Retry";
        String body = template != null ? template.getBodyTemplate() : "Retried Notification";

        // Dispatch retry
        SendResult sendResult = channelDispatcher.dispatch(
                notifLog.getChannel(),
                null,
                subject,
                body,
                false
        );

        if (sendResult.isSuccessful()) {
            notifLog.recordAttempt(NotificationDeliveryStatus.SENT);
            log.info("Successfully retried notification log ID: {}", notifLog.getId());
        } else {
            notifLog.recordAttempt(NotificationDeliveryStatus.FAILED);
            outboxService.recordEvent(
                    "NotificationLog",
                    notifLog.getId().toString(),
                    "NotificationDeliveryFailed",
                    new NotificationDeliveryFailedEvent(notifLog.getId(), sendResult.getErrorMessage())
            );
            log.warn("Retry failed for notification log ID: {}, reason: {}", notifLog.getId(), sendResult.getErrorMessage());
        }

        NotificationLog saved = logRepository.save(notifLog);
        return toLogDto(saved);
    }

    // =========================================================================
    // 3. Dispatch Notification (FR-013, NFR-FAULTISO-001)
    // =========================================================================

    @Override
    @Transactional
    public DispatchNotificationResponse dispatchNotification(DispatchNotificationRequest request) {
        String channel = (request.getChannel() != null && !request.getChannel().isBlank())
                ? request.getChannel().toUpperCase()
                : "EMAIL";

        // Find active template for event code and channel
        Optional<NotificationTemplate> templateOpt = templateRepository.findByEventCodeAndChannelAndStatus(
                request.getEventCode(), channel, TemplateStatus.ACTIVE
        );

        if (templateOpt.isEmpty()) {
            templateOpt = templateRepository.findFirstByEventCodeAndStatus(request.getEventCode(), TemplateStatus.ACTIVE);
        }

        if (templateOpt.isEmpty()) {
            log.warn("No active notification template found for eventCode: {} and channel: {}", request.getEventCode(), channel);
            return new DispatchNotificationResponse(
                    null,
                    NotificationDeliveryStatus.FAILED.name(),
                    channel,
                    request.getEventCode(),
                    null,
                    null,
                    "No active template found for event code: " + request.getEventCode()
            );
        }

        NotificationTemplate template = templateOpt.get();

        // Prepare parameters
        Map<String, Object> params = new HashMap<>();
        if (request.getParameters() != null) {
            params.putAll(request.getParameters());
        }
        if (request.getOrderId() != null) {
            params.putIfAbsent("order_id", request.getOrderId().toString());
            params.putIfAbsent("orderId", request.getOrderId().toString());
        }
        if (request.getCustomerId() != null) {
            params.putIfAbsent("customer_id", request.getCustomerId().toString());
            params.putIfAbsent("customerId", request.getCustomerId().toString());
        }

        // Render subject and body
        String renderedSubject = templateRenderer.render(template.getSubject(), params);
        String renderedBody = templateRenderer.render(template.getBodyTemplate(), params);

        // Dispatch via channel
        SendResult sendResult = channelDispatcher.dispatch(
                channel,
                request.getRecipient(),
                renderedSubject,
                renderedBody,
                request.isSimulateFailure()
        );

        NotificationDeliveryStatus status = sendResult.isSuccessful()
                ? NotificationDeliveryStatus.SENT
                : NotificationDeliveryStatus.FAILED;

        // Persist NotificationLog
        NotificationLog notifLog = new NotificationLog(
                request.getOrderId(),
                request.getCustomerId(),
                template.getId(),
                channel,
                status
        );
        NotificationLog savedLog = logRepository.save(notifLog);

        // Fault isolation & Outbox recording
        if (!sendResult.isSuccessful()) {
            outboxService.recordEvent(
                    "NotificationLog",
                    savedLog.getId().toString(),
                    "NotificationDeliveryFailed",
                    new NotificationDeliveryFailedEvent(savedLog.getId(), sendResult.getErrorMessage())
            );
            log.warn("Notification dispatch failed for event [{}], logId [{}]. Outbox event emitted.",
                    request.getEventCode(), savedLog.getId());
        } else {
            log.info("Notification successfully dispatched for event [{}], logId [{}]",
                    request.getEventCode(), savedLog.getId());
        }

        return new DispatchNotificationResponse(
                savedLog.getId(),
                status.name(),
                channel,
                request.getEventCode(),
                renderedSubject,
                renderedBody,
                sendResult.getErrorMessage()
        );
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private NotificationTemplateDto toTemplateDto(NotificationTemplate t) {
        return new NotificationTemplateDto(
                t.getId(),
                t.getEventCode(),
                t.getChannel(),
                t.getSubject(),
                t.getBodyTemplate(),
                t.getStatus() != null ? t.getStatus().name() : TemplateStatus.ACTIVE.name()
        );
    }

    private NotificationLogDto toLogDto(NotificationLog l) {
        return new NotificationLogDto(
                l.getId(),
                l.getOrderId(),
                l.getCustomerId(),
                l.getTemplateId(),
                l.getChannel(),
                l.getStatus() != null ? l.getStatus().name() : NotificationDeliveryStatus.SENT.name(),
                l.getAttemptCount(),
                l.getLastAttemptAt()
        );
    }
}
