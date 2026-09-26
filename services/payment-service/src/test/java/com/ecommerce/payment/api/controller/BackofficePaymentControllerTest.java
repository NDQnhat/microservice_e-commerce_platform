package com.ecommerce.payment.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.payment.api.dto.ManualReconcileRequest;
import com.ecommerce.payment.api.dto.PaymentTransactionDto;
import com.ecommerce.payment.domain.model.PaymentStatus;
import com.ecommerce.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
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
class BackofficePaymentControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private BackofficePaymentController backofficePaymentController;

    private UUID transactionId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(backofficePaymentController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        transactionId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/payments/anomalies: returns 200 OK with paged anomalies")
    void testGetAnomalies_Success() throws Exception {
        PaymentTransactionDto dto = new PaymentTransactionDto(
                transactionId, UUID.randomUUID(), "GW-FAIL", new BigDecimal("100000.00"),
                "FAILED", Instant.now(), Instant.now()
        );
        Page<PaymentTransactionDto> page = new PageImpl<>(List.of(dto), org.springframework.data.domain.PageRequest.of(0, 10), 1);

        when(paymentService.getAnomalies(eq(PaymentStatus.FAILED), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/backoffice/payments/anomalies")
                        .param("status", "FAILED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(transactionId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("FAILED"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/payments/{id}/reconcile: valid request returns 200 OK")
    void testManualReconcile_Success() throws Exception {
        ManualReconcileRequest request = new ManualReconcileRequest("BANK-TXN-12345", "Bank confirmation slip verified");
        PaymentTransactionDto dto = new PaymentTransactionDto(
                transactionId, UUID.randomUUID(), "GW-FAIL", new BigDecimal("100000.00"),
                "SUCCEEDED", Instant.now(), Instant.now(), "BANK-TXN-12345", "Bank confirmation slip verified"
        );

        when(paymentService.reconcilePayment(eq(transactionId), any(ManualReconcileRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/payments/{transactionId}/reconcile", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.evidence_reference").value("BANK-TXN-12345"))
                .andExpect(jsonPath("$.reconciliation_reason").value("Bank confirmation slip verified"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/payments/{id}/reconcile: missing evidence returns 400 Bad Request")
    void testManualReconcile_ValidationError() throws Exception {
        ManualReconcileRequest request = new ManualReconcileRequest("", "Reason only");

        mockMvc.perform(post("/api/v1/backoffice/payments/{transactionId}/reconcile", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/payments/{id}/reconcile: BR-012 violation returns 422 Unprocessable Entity")
    void testManualReconcile_BR012Violation() throws Exception {
        ManualReconcileRequest request = new ManualReconcileRequest("EVID-001", "Some Reason");

        when(paymentService.reconcilePayment(eq(transactionId), any(ManualReconcileRequest.class)))
                .thenThrow(new BusinessRuleException("BR-012", "Manual reconciliation requires evidence reference and logged reason."));

        mockMvc.perform(post("/api/v1/backoffice/payments/{transactionId}/reconcile", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violated_rule").value("BR-012"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/payments/{id}/reconcile: non-existent transaction returns 404 Not Found")
    void testManualReconcile_NotFound() throws Exception {
        ManualReconcileRequest request = new ManualReconcileRequest("EVID-001", "Some Reason");

        when(paymentService.reconcilePayment(eq(transactionId), any(ManualReconcileRequest.class)))
                .thenThrow(new NotFoundException("Payment transaction not found: " + transactionId));

        mockMvc.perform(post("/api/v1/backoffice/payments/{transactionId}/reconcile", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/payments/timeout-check: returns 200 OK")
    void testTriggerTimeoutCheck() throws Exception {
        when(paymentService.checkTimeouts(20)).thenReturn(5);

        mockMvc.perform(post("/api/v1/backoffice/payments/timeout-check")
                        .param("timeoutMinutes", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(5))
                .andExpect(jsonPath("$.timeoutMinutes").value(20));
    }
}
