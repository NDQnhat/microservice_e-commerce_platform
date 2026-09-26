package com.ecommerce.notification.api.controller;

import com.ecommerce.notification.api.dto.DispatchNotificationRequest;
import com.ecommerce.notification.api.dto.DispatchNotificationResponse;
import com.ecommerce.notification.api.dto.NotificationLogDto;
import com.ecommerce.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/dispatch")
    public ResponseEntity<DispatchNotificationResponse> dispatch(
            @Valid @RequestBody DispatchNotificationRequest request) {
        DispatchNotificationResponse response = notificationService.dispatchNotification(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logs/{logId}")
    public ResponseEntity<NotificationLogDto> getLog(@PathVariable UUID logId) {
        NotificationLogDto logDto = notificationService.getLog(logId);
        return ResponseEntity.ok(logDto);
    }
}
