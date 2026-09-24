package com.ecommerce.notification.api.controller;

import com.ecommerce.notification.api.dto.NotificationLogDto;
import com.ecommerce.notification.domain.model.NotificationDeliveryStatus;
import com.ecommerce.notification.domain.model.NotificationLog;
import com.ecommerce.notification.domain.repository.NotificationLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationLogRepository logRepository;

    public NotificationController(NotificationLogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping("/logs/{logId}")
    public ResponseEntity<NotificationLogDto> getLog(@PathVariable UUID logId) {
        NotificationLog log = logRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found"));

        return ResponseEntity.ok(new NotificationLogDto(
                log.getId(),
                log.getOrderId(),
                log.getCustomerId(),
                log.getTemplateId(),
                log.getChannel(),
                log.getStatus().name(),
                log.getAttemptCount(),
                log.getLastAttemptAt()
        ));
    }
}
