package com.ecommerce.notification.api.controller;

import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.notification.api.dto.CreateTemplateRequest;
import com.ecommerce.notification.api.dto.NotificationTemplateDto;
import com.ecommerce.notification.api.dto.UpdateTemplateRequest;
import com.ecommerce.notification.domain.model.TemplateStatus;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BackofficeNotificationTemplateControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BackofficeNotificationTemplateController controller;

    private UUID templateId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        templateId = UUID.randomUUID();
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/notification-templates: creates template and returns 201 Created (API-NOTI-001)")
    void createTemplate_returns201() throws Exception {
        CreateTemplateRequest request = new CreateTemplateRequest(
                "ORDER_CREATED", "EMAIL", "Order Confirmation", "Thank you {{name}}");

        NotificationTemplateDto dto = new NotificationTemplateDto(
                templateId, "ORDER_CREATED", "EMAIL", "Order Confirmation", "Thank you {{name}}", "ACTIVE");

        when(notificationService.createTemplate(any(CreateTemplateRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/notification-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(templateId.toString()))
                .andExpect(jsonPath("$.eventCode").value("ORDER_CREATED"))
                .andExpect(jsonPath("$.channel").value("EMAIL"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/notification-templates: missing required fields returns 400 Bad Request")
    void createTemplate_validationError_returns400() throws Exception {
        CreateTemplateRequest invalidRequest = new CreateTemplateRequest("", "", "", "");

        mockMvc.perform(post("/api/v1/backoffice/notification-templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PUT /api/v1/backoffice/notification-templates/{templateId}: updates template and returns 200 OK")
    void updateTemplate_returns200() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest("New Subject", "New Body", TemplateStatus.INACTIVE);

        NotificationTemplateDto dto = new NotificationTemplateDto(
                templateId, "ORDER_CREATED", "EMAIL", "New Subject", "New Body", "INACTIVE");

        when(notificationService.updateTemplate(eq(templateId), any(UpdateTemplateRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/v1/backoffice/notification-templates/{templateId}", templateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("New Subject"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("PUT /api/v1/backoffice/notification-templates/{templateId}: template not found returns 404")
    void updateTemplate_notFound_returns404() throws Exception {
        UpdateTemplateRequest request = new UpdateTemplateRequest("New Subject", "New Body", TemplateStatus.ACTIVE);

        when(notificationService.updateTemplate(eq(templateId), any(UpdateTemplateRequest.class)))
                .thenThrow(new NotFoundException("Notification template not found: " + templateId));

        mockMvc.perform(put("/api/v1/backoffice/notification-templates/{templateId}", templateId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/notification-templates: returns 200 with Page of templates")
    void getTemplates_returns200() throws Exception {
        NotificationTemplateDto dto = new NotificationTemplateDto(
                templateId, "ORDER_CREATED", "EMAIL", "Subj", "Body", "ACTIVE");

        when(notificationService.getTemplates(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), org.springframework.data.domain.PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/backoffice/notification-templates")
                        .param("eventCode", "ORDER_CREATED")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventCode").value("ORDER_CREATED"));
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/notification-templates/{templateId}: returns 200 with template detail")
    void getTemplate_returns200() throws Exception {
        NotificationTemplateDto dto = new NotificationTemplateDto(
                templateId, "ORDER_CREATED", "EMAIL", "Subj", "Body", "ACTIVE");

        when(notificationService.getTemplate(templateId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/backoffice/notification-templates/{templateId}", templateId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(templateId.toString()));
    }
}
