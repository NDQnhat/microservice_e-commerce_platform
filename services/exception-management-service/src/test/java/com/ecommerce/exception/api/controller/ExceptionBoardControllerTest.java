package com.ecommerce.exception.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.InvalidStateException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.exception.api.dto.CreateExceptionRecordRequest;
import com.ecommerce.exception.api.dto.DashboardSummaryDto;
import com.ecommerce.exception.api.dto.ExceptionRecordDto;
import com.ecommerce.exception.api.dto.ResolveExceptionRequest;
import com.ecommerce.exception.domain.model.ExceptionRecordStatus;
import com.ecommerce.exception.domain.model.ExceptionType;
import com.ecommerce.exception.service.ExceptionService;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExceptionBoardControllerTest {

    private MockMvc mockMvc;
    private MockMvc adminDashboardMockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ExceptionService exceptionService;

    @InjectMocks
    private ExceptionBoardController controller;

    private AdminDashboardController adminDashboardController;

    private UUID exceptionId;
    private ExceptionRecordDto sampleDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        adminDashboardController = new AdminDashboardController(exceptionService);
        adminDashboardMockMvc = MockMvcBuilders.standaloneSetup(adminDashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        exceptionId = UUID.randomUUID();
        sampleDto = new ExceptionRecordDto(
                exceptionId,
                ExceptionType.PAYMENT_FAILED,
                "payment-service",
                "ORD-999",
                "ORDER",
                "PAY_ERR",
                "Insufficient funds",
                null,
                ExceptionRecordStatus.OPEN,
                null,
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/exceptions: returns 200 OK with paginated records (API-EXC-001)")
    void listExceptions_primaryRoute_returns200() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10);
        when(exceptionService.listExceptions(eq(ExceptionRecordStatus.OPEN), eq(ExceptionType.PAYMENT_FAILED), eq("ORDER"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleDto), pageable, 1));

        mockMvc.perform(get("/api/v1/backoffice/exceptions")
                        .param("status", "OPEN")
                        .param("exceptionType", "PAYMENT_FAILED")
                        .param("referenceType", "ORDER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(exceptionId.toString()))
                .andExpect(jsonPath("$.content[0].exceptionType").value("PAYMENT_FAILED"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /api/v1/exceptions: alias returns 200 OK (Frontend ExceptionsPage.tsx)")
    void listExceptions_aliasRoute_returns200() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10);
        when(exceptionService.listExceptions(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleDto), pageable, 1));

        mockMvc.perform(get("/api/v1/exceptions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(exceptionId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/exceptions/{id}: returns 200 OK when found")
    void getExceptionById_found() throws Exception {
        when(exceptionService.getExceptionById(exceptionId)).thenReturn(sampleDto);

        mockMvc.perform(get("/api/v1/backoffice/exceptions/{id}", exceptionId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exceptionId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/exceptions/{id}: returns 404 NOT_FOUND when missing")
    void getExceptionById_notFound_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(exceptionService.getExceptionById(unknownId))
                .thenThrow(new NotFoundException("Exception record not found: " + unknownId));

        mockMvc.perform(get("/api/v1/exceptions/{id}", unknownId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/exceptions: creates new exception record returning 201 Created")
    void recordException_returns201() throws Exception {
        CreateExceptionRecordRequest request = new CreateExceptionRecordRequest(
                ExceptionType.STUCK_ORDER,
                "order-service",
                "ORD-777",
                "ORDER",
                "TIMEOUT",
                "Order timeout",
                "{}"
        );

        when(exceptionService.recordException(any(CreateExceptionRecordRequest.class))).thenReturn(sampleDto);

        mockMvc.perform(post("/api/v1/backoffice/exceptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(exceptionId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/exceptions: returns 400 VALIDATION_ERROR when fields invalid")
    void recordException_invalid_returns400() throws Exception {
        CreateExceptionRecordRequest invalid = new CreateExceptionRecordRequest(
                null, "", "", "", "", "", ""
        );

        mockMvc.perform(post("/api/v1/backoffice/exceptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/exceptions/{id}/assign: assigns operator and returns 200 OK")
    void assignException_returns200() throws Exception {
        ExceptionRecordDto assignedDto = new ExceptionRecordDto(
                exceptionId,
                ExceptionType.PAYMENT_FAILED,
                "payment-service",
                "ORD-999",
                "ORDER",
                "PAY_ERR",
                "Insufficient funds",
                null,
                ExceptionRecordStatus.INVESTIGATING,
                "OPERATOR_ALICE",
                null,
                null,
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(exceptionService.assignException(eq(exceptionId), any())).thenReturn(assignedDto);

        mockMvc.perform(post("/api/v1/backoffice/exceptions/{id}/assign", exceptionId)
                        .header("X-User-Id", "OPERATOR_ALICE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INVESTIGATING"))
                .andExpect(jsonPath("$.assignedTo").value("OPERATOR_ALICE"));
    }

    @Test
    @DisplayName("POST /api/v1/exceptions/{id}/resolve: resolves exception and returns 200 OK (BR-017, BR-018)")
    void resolveException_returns200() throws Exception {
        ResolveExceptionRequest request = new ResolveExceptionRequest("RETRY_PAYMENT", "Payment verified with bank");
        ExceptionRecordDto resolvedDto = new ExceptionRecordDto(
                exceptionId,
                ExceptionType.PAYMENT_FAILED,
                "payment-service",
                "ORD-999",
                "ORDER",
                "PAY_ERR",
                "Insufficient funds",
                null,
                ExceptionRecordStatus.RESOLVED,
                null,
                "ADMIN_BOB",
                Instant.now(),
                "RETRY_PAYMENT",
                "Payment verified with bank",
                Instant.now(),
                Instant.now()
        );

        when(exceptionService.resolveException(eq(exceptionId), any(), any())).thenReturn(resolvedDto);

        mockMvc.perform(post("/api/v1/exceptions/{id}/resolve", exceptionId)
                        .header("X-User-Id", "ADMIN_BOB")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedBy").value("ADMIN_BOB"))
                .andExpect(jsonPath("$.resolutionAction").value("RETRY_PAYMENT"));
    }

    @Test
    @DisplayName("POST /api/v1/exceptions/{id}/resolve: returns 422 BUSINESS_RULE_VIOLATION when notes missing")
    void resolveException_missingNotes_returns422() throws Exception {
        ResolveExceptionRequest request = new ResolveExceptionRequest("RETRY_PAYMENT", "notes");

        when(exceptionService.resolveException(eq(exceptionId), any(), any()))
                .thenThrow(new BusinessRuleException("BR-018", "Resolution notes are required when resolving an exception record (BR-018)"));

        mockMvc.perform(post("/api/v1/exceptions/{id}/resolve", exceptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violated_rule").value("BR-018"))
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    @DisplayName("POST /api/v1/exceptions/{id}/resolve: returns 409 INVALID_STATE when already terminal")
    void resolveException_terminalState_returns409() throws Exception {
        ResolveExceptionRequest request = new ResolveExceptionRequest("RETRY_PAYMENT", "notes");

        when(exceptionService.resolveException(eq(exceptionId), any(), any()))
                .thenThrow(new InvalidStateException("Terminal exception status RESOLVED cannot transition to RESOLVED"));

        mockMvc.perform(post("/api/v1/exceptions/{id}/resolve", exceptionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/v1/exceptions/{id}/ignore: ignores exception and returns 200 OK")
    void ignoreException_returns200() throws Exception {
        ExceptionRecordDto ignoredDto = new ExceptionRecordDto(
                exceptionId,
                ExceptionType.PAYMENT_FAILED,
                "payment-service",
                "ORD-999",
                "ORDER",
                "PAY_ERR",
                "Insufficient funds",
                null,
                ExceptionRecordStatus.IGNORED,
                null,
                "OPERATOR_ALICE",
                Instant.now(),
                "IGNORED",
                "Duplicate trigger",
                Instant.now(),
                Instant.now()
        );

        when(exceptionService.ignoreException(eq(exceptionId), any(), any())).thenReturn(ignoredDto);

        mockMvc.perform(post("/api/v1/exceptions/{id}/ignore", exceptionId)
                        .header("X-User-Id", "OPERATOR_ALICE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "Duplicate trigger"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IGNORED"))
                .andExpect(jsonPath("$.resolutionAction").value("IGNORED"));
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/dashboard/summary: returns 200 OK with dashboard telemetry (API-DASH-001)")
    void getDashboardSummary_adminRoute_returns200() throws Exception {
        DashboardSummaryDto summary = new DashboardSummaryDto(
                12L, 3L, 45L, Map.of("PAYMENT_FAILED", 8L, "STUCK_ORDER", 4L)
        );

        when(exceptionService.getDashboardSummary()).thenReturn(summary);

        adminDashboardMockMvc.perform(get("/api/v1/backoffice/dashboard/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOpenExceptions").value(12))
                .andExpect(jsonPath("$.totalInvestigatingExceptions").value(3))
                .andExpect(jsonPath("$.totalResolvedExceptions").value(45))
                .andExpect(jsonPath("$.openExceptionsByType.PAYMENT_FAILED").value(8));
    }

    @Test
    @DisplayName("GET /api/v1/exceptions/dashboard/summary: alias returns 200 OK (Frontend DashboardPage.tsx)")
    void getDashboardSummary_frontendAlias_returns200() throws Exception {
        DashboardSummaryDto summary = new DashboardSummaryDto(
                12L, 3L, 45L, Map.of("PAYMENT_FAILED", 8L)
        );

        when(exceptionService.getDashboardSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/v1/exceptions/dashboard/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOpenExceptions").value(12));
    }
}
