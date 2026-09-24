package com.ecommerce.notification.api.controller;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.notification.api.dto.CreateTemplateRequest;
import com.ecommerce.notification.api.dto.NotificationLogDto;
import com.ecommerce.notification.api.dto.NotificationTemplateDto;
import com.ecommerce.notification.domain.model.NotificationDeliveryStatus;
import com.ecommerce.notification.domain.model.NotificationLog;
import com.ecommerce.notification.domain.model.NotificationTemplate;
import com.ecommerce.notification.domain.repository.NotificationLogRepository;
import com.ecommerce.notification.domain.repository.NotificationTemplateRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/notifications")
public class BackofficeNotificationController {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;

    public BackofficeNotificationController(NotificationTemplateRepository templateRepository,
                                            NotificationLogRepository logRepository) {
        this.templateRepository = templateRepository;
        this.logRepository = logRepository;
    }

    @PostMapping("/templates")
    @Transactional
    public ResponseEntity<NotificationTemplateDto> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        NotificationTemplate template = new NotificationTemplate(
                request.getEventCode(),
                request.getChannel(),
                request.getSubject(),
                request.getBodyTemplate()
        );
        NotificationTemplate saved = templateRepository.save(template);

        return ResponseEntity.status(HttpStatus.CREATED).body(new NotificationTemplateDto(
                saved.getId(),
                saved.getEventCode(),
                saved.getChannel(),
                saved.getSubject(),
                saved.getBodyTemplate(),
                saved.getStatus().name()
        ));
    }

    @GetMapping("/logs")
    public ResponseEntity<Page<NotificationLogDto>> getLogs(
            @RequestParam(required = false) NotificationDeliveryStatus status,
            Pageable pageable) {

        Page<NotificationLog> logs = status != null
                ? logRepository.findByStatus(status, pageable)
                : logRepository.findAll(pageable);

        return ResponseEntity.ok(logs.map(l -> new NotificationLogDto(
                l.getId(),
                l.getOrderId(),
                l.getCustomerId(),
                l.getTemplateId(),
                l.getChannel(),
                l.getStatus().name(),
                l.getAttemptCount(),
                l.getLastAttemptAt()
        )));
    }

    @PostMapping("/logs/{logId}/retry")
    @Transactional
    public ResponseEntity<NotificationLogDto> retryNotification(@PathVariable UUID logId) {
        NotificationLog log = logRepository.findById(logId)
                .orElseThrow(() -> new NotFoundException("Notification log not found: " + logId));

        log.setStatus(NotificationDeliveryStatus.RETRIED);
        log.setAttemptCount(log.getAttemptCount() + 1);
        log.setLastAttemptAt(Instant.now());
        NotificationLog saved = logRepository.save(log);

        return ResponseEntity.ok(new NotificationLogDto(
                saved.getId(),
                saved.getOrderId(),
                saved.getCustomerId(),
                saved.getTemplateId(),
                saved.getChannel(),
                saved.getStatus().name(),
                saved.getAttemptCount(),
                saved.getLastAttemptAt()
        ));
    }
}
