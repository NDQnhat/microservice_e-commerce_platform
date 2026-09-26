package com.ecommerce.notification.api.controller;

import com.ecommerce.notification.api.dto.CreateTemplateRequest;
import com.ecommerce.notification.api.dto.NotificationLogDto;
import com.ecommerce.notification.api.dto.NotificationTemplateDto;
import com.ecommerce.notification.domain.model.NotificationDeliveryStatus;
import com.ecommerce.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/notifications")
public class BackofficeNotificationController {

    private final NotificationService notificationService;

    public BackofficeNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Backward-compatible template creation endpoint under /notifications/templates.
     */
    @PostMapping("/templates")
    public ResponseEntity<NotificationTemplateDto> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        NotificationTemplateDto created = notificationService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Queries notification delivery logs (supports /api/v1/backoffice/notifications and /logs).
     */
    @GetMapping({"", "/logs"})
    public ResponseEntity<Page<NotificationLogDto>> getLogs(
            @RequestParam(required = false) NotificationDeliveryStatus status,
            @RequestParam(required = false) UUID orderId,
            Pageable pageable) {
        Page<NotificationLogDto> logs = notificationService.getLogs(status, orderId, pageable);
        return ResponseEntity.ok(logs);
    }

    /**
     * Triggers manual retry for a notification log (supports /{logId}/retry and /logs/{logId}/retry).
     */
    @PostMapping({"/{logId}/retry", "/logs/{logId}/retry"})
    public ResponseEntity<NotificationLogDto> retryNotification(@PathVariable UUID logId) {
        NotificationLogDto retried = notificationService.retryNotification(logId);
        return ResponseEntity.ok(retried);
    }
}
