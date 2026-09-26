package com.ecommerce.audit.api.controller;

import com.ecommerce.audit.api.dto.AuditLogDto;
import com.ecommerce.audit.api.dto.RecordAuditRequest;
import com.ecommerce.audit.domain.model.AuditActionType;
import com.ecommerce.audit.service.AuditService;
import com.ecommerce.audit.service.dto.AuditLogFilter;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditLogController controller;

    private UUID logId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        logId = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/audit-log: returns 200 OK with paginated logs (FR-034)")
    void shouldQueryAuditLogs() throws Exception {
        AuditLogDto dto = new AuditLogDto(
                logId,
                actorId,
                "SUPER_ADMIN",
                "CONFIG_CHANGE",
                "BUSINESS_CONFIGURATION",
                "RESERVATION_TTL_MINUTES",
                "15",
                "30",
                "Updated TTL",
                Instant.now()
        );

        PageRequest pageable = PageRequest.of(0, 10);
        when(auditService.queryAuditLogs(any(AuditLogFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), pageable, 1));

        mockMvc.perform(get("/api/v1/backoffice/audit-log")
                        .param("actorRole", "SUPER_ADMIN")
                        .param("actionType", "CONFIG_CHANGE")
                        .param("entityType", "BUSINESS_CONFIGURATION")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(logId.toString()))
                .andExpect(jsonPath("$.content[0].actorRole").value("SUPER_ADMIN"))
                .andExpect(jsonPath("$.content[0].actionType").value("CONFIG_CHANGE"));
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/audit-logs: alias returns 200 OK")
    void shouldQueryAuditLogsAlias() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10);
        when(auditService.queryAuditLogs(any(AuditLogFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        mockMvc.perform(get("/api/v1/backoffice/audit-logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/audit-log: records audit log and returns 201 Created")
    void shouldRecordAuditLog() throws Exception {
        RecordAuditRequest request = new RecordAuditRequest(
                actorId,
                "SUPER_ADMIN",
                AuditActionType.ROLE_ASSIGN,
                "USER",
                UUID.randomUUID().toString(),
                null,
                "ROLE_ADMIN",
                "Assigned admin role"
        );

        AuditLogDto responseDto = new AuditLogDto(
                logId,
                actorId,
                "SUPER_ADMIN",
                "ROLE_ASSIGN",
                "USER",
                request.getEntityId(),
                null,
                "ROLE_ADMIN",
                "Assigned admin role",
                Instant.now()
        );

        when(auditService.recordAuditLog(any(RecordAuditRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/backoffice/audit-log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(logId.toString()))
                .andExpect(jsonPath("$.actionType").value("ROLE_ASSIGN"));
    }

    @Test
    @DisplayName("POST /api/v1/audit-logs/record: direct ingestion endpoint alias returns 201 Created")
    void shouldRecordAuditLogAlias() throws Exception {
        RecordAuditRequest request = new RecordAuditRequest(
                actorId,
                "SUPER_ADMIN",
                AuditActionType.INVENTORY_ADJUST,
                "INVENTORY",
                "SKU-100",
                "10",
                "20",
                "Restock"
        );

        AuditLogDto responseDto = new AuditLogDto(
                logId,
                actorId,
                "SUPER_ADMIN",
                "INVENTORY_ADJUST",
                "INVENTORY",
                "SKU-100",
                "10",
                "20",
                "Restock",
                Instant.now()
        );

        when(auditService.recordAuditLog(any(RecordAuditRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/audit-logs/record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actionType").value("INVENTORY_ADJUST"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/audit-log: returns 400 VALIDATION_ERROR on missing mandatory fields")
    void shouldReturn400OnValidationError() throws Exception {
        RecordAuditRequest invalidRequest = new RecordAuditRequest(
                null, // Missing actorId
                "",   // Blank actorRole
                null, // Missing actionType
                "",   // Blank entityType
                "",   // Blank entityId
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/backoffice/audit-log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/audit-log: returns 422 BUSINESS_RULE_VIOLATION when BR-012 violated")
    void shouldReturn422OnBusinessRuleViolation() throws Exception {
        RecordAuditRequest request = new RecordAuditRequest(
                actorId,
                "ORDER_OPERATIONS_ADMIN",
                AuditActionType.PAYMENT_RECONCILE,
                "PAYMENT_TRANSACTION",
                "TXN-1",
                "TIMEOUT",
                "SUCCEEDED",
                null
        );

        when(auditService.recordAuditLog(any(RecordAuditRequest.class)))
                .thenThrow(new BusinessRuleException("BR-012", "Reason is mandatory for PAYMENT_RECONCILE audit action"));

        mockMvc.perform(post("/api/v1/backoffice/audit-log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violated_rule").value("BR-012"))
                .andExpect(jsonPath("$.status").value(422));
    }
}
