package com.ecommerce.notification.api.controller;

import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.notification.api.dto.CreateTemplateRequest;
import com.ecommerce.notification.api.dto.NotificationLogDto;
import com.ecommerce.notification.api.dto.NotificationTemplateDto;
import com.ecommerce.notification.domain.model.NotificationDeliveryStatus;
import com.ecommerce.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BackofficeNotificationControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BackofficeNotificationController controller;

    private UUID logId;
    private UUID orderId;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        logId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        templateId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/notifications/templates: legacy template create returns 201 Created")
    void createTemplateLegacy_returns201() throws Exception {
        CreateTemplateRequest request = new CreateTemplateRequest(
                "ORDER_CREATED", "EMAIL", "Order Confirmation", "Thank you {{name}}");

        NotificationTemplateDto dto = new NotificationTemplateDto(
                templateId, "ORDER_CREATED", "EMAIL", "Order Confirmation", "Thank you {{name}}", "ACTIVE");

        when(notificationService.createTemplate(any(CreateTemplateRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/notifications/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(templateId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/notifications: returns 200 with Page of logs (API-NOTI-002)")
    void getLogs_returns200() throws Exception {
        NotificationLogDto dto = new NotificationLogDto(
                logId, orderId, UUID.randomUUID(), templateId, "EMAIL", "SENT", 1, Instant.now());

        when(notificationService.getLogs(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), org.springframework.data.domain.PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/backoffice/notifications")
                        .param("status", "SENT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(logId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("SENT"));
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/notifications/logs: alias path returns 200 with Page of logs")
    void getLogs_aliasPath_returns200() throws Exception {
        NotificationLogDto dto = new NotificationLogDto(
                logId, orderId, UUID.randomUUID(), templateId, "EMAIL", "FAILED", 1, Instant.now());

        when(notificationService.getLogs(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), org.springframework.data.domain.PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/backoffice/notifications/logs")
                        .param("orderId", orderId.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(logId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/notifications/{logId}/retry: triggers manual retry and returns 200 (FR-036)")
    void retryNotification_returns200() throws Exception {
        NotificationLogDto dto = new NotificationLogDto(
                logId, orderId, UUID.randomUUID(), templateId, "EMAIL", "SENT", 2, Instant.now());

        when(notificationService.retryNotification(logId)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/notifications/{logId}/retry", logId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(logId.toString()))
                .andExpect(jsonPath("$.attemptCount").value(2))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/notifications/logs/{logId}/retry: alias retry path returns 200")
    void retryNotification_aliasPath_returns200() throws Exception {
        NotificationLogDto dto = new NotificationLogDto(
                logId, orderId, UUID.randomUUID(), templateId, "EMAIL", "SENT", 2, Instant.now());

        when(notificationService.retryNotification(logId)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/notifications/logs/{logId}/retry", logId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(logId.toString()))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/notifications/{logId}/retry: log not found returns 404")
    void retryNotification_notFound_returns404() throws Exception {
        when(notificationService.retryNotification(logId))
                .thenThrow(new NotFoundException("Notification log not found: " + logId));

        mockMvc.perform(post("/api/v1/backoffice/notifications/{logId}/retry", logId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
