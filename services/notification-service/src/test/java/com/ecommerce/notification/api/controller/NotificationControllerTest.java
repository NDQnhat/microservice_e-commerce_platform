package com.ecommerce.notification.api.controller;

import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.notification.api.dto.DispatchNotificationRequest;
import com.ecommerce.notification.api.dto.DispatchNotificationResponse;
import com.ecommerce.notification.api.dto.NotificationLogDto;
import com.ecommerce.notification.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private UUID logId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        logId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/notifications/dispatch: returns 200 with DispatchNotificationResponse")
    void dispatch_returns200() throws Exception {
        DispatchNotificationRequest request = new DispatchNotificationRequest(
                "ORDER_CREATED", "EMAIL", orderId, UUID.randomUUID(), "test@example.com", Map.of("name", "Alice"), false);

        DispatchNotificationResponse response = new DispatchNotificationResponse(
                logId, "SENT", "EMAIL", "ORDER_CREATED", "Order Confirmation", "Hello Alice", null);

        when(notificationService.dispatchNotification(any(DispatchNotificationRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/notifications/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationLogId").value(logId.toString()))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.renderedSubject").value("Order Confirmation"));
    }

    @Test
    @DisplayName("POST /api/v1/notifications/dispatch: missing eventCode returns 400 Bad Request")
    void dispatch_validationError_returns400() throws Exception {
        DispatchNotificationRequest invalidRequest = new DispatchNotificationRequest();

        mockMvc.perform(post("/api/v1/notifications/dispatch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/logs/{logId}: returns 200 with NotificationLogDto")
    void getLog_returns200() throws Exception {
        NotificationLogDto logDto = new NotificationLogDto(
                logId, orderId, UUID.randomUUID(), UUID.randomUUID(), "EMAIL", "SENT", 1, Instant.now());

        when(notificationService.getLog(logId)).thenReturn(logDto);

        mockMvc.perform(get("/api/v1/notifications/logs/{logId}", logId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(logId.toString()))
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/logs/{logId}: not found returns 404 RFC 7807")
    void getLog_notFound_returns404() throws Exception {
        when(notificationService.getLog(logId))
                .thenThrow(new NotFoundException("Notification log not found: " + logId));

        mockMvc.perform(get("/api/v1/notifications/logs/{logId}", logId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
