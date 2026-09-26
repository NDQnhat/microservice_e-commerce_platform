package com.ecommerce.notification.api.controller;

import com.ecommerce.notification.api.dto.CreateTemplateRequest;
import com.ecommerce.notification.api.dto.NotificationTemplateDto;
import com.ecommerce.notification.api.dto.UpdateTemplateRequest;
import com.ecommerce.notification.domain.model.TemplateStatus;
import com.ecommerce.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/notification-templates")
public class BackofficeNotificationTemplateController {

    private final NotificationService notificationService;

    public BackofficeNotificationTemplateController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationTemplateDto> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        NotificationTemplateDto created = notificationService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<NotificationTemplateDto> updateTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateTemplateRequest request) {
        NotificationTemplateDto updated = notificationService.updateTemplate(templateId, request);
        return ResponseEntity.ok(updated);
    }

    @GetMapping
    public ResponseEntity<Page<NotificationTemplateDto>> getTemplates(
            @RequestParam(required = false) String eventCode,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) TemplateStatus status,
            Pageable pageable) {
        Page<NotificationTemplateDto> page = notificationService.getTemplates(eventCode, channel, status, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<NotificationTemplateDto> getTemplate(@PathVariable UUID templateId) {
        NotificationTemplateDto template = notificationService.getTemplate(templateId);
        return ResponseEntity.ok(template);
    }
}
