package com.ecommerce.payment.api.controller;

import com.ecommerce.common.error.AuthenticationFailedException;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.payment.api.dto.InitiatePaymentRequest;
import com.ecommerce.payment.api.dto.PaymentCallbackRequest;
import com.ecommerce.payment.api.dto.PaymentTransactionDto;
import com.ecommerce.payment.service.PaymentService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private final String validSecret = "gateway-shared-secret-12345";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/payments/initiate: valid request returns 201 Created")
    void testInitiatePayment_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        InitiatePaymentRequest request = new InitiatePaymentRequest(orderId, new BigDecimal("200000.00"));
        PaymentTransactionDto dto = new PaymentTransactionDto(
                txId, orderId, null, new BigDecimal("200000.00"), "INITIATED", Instant.now(), null
        );

        when(paymentService.initiatePayment(any(InitiatePaymentRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(txId.toString()))
                .andExpect(jsonPath("$.order_id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("INITIATED"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/initiate: invalid request returns 400 Bad Request")
    void testInitiatePayment_ValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/callback: valid SUCCESS callback returns 200 OK")
    void testHandleCallback_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        UUID txId = UUID.randomUUID();
        PaymentCallbackRequest request = new PaymentCallbackRequest(
                orderId, "GW-999", "SUCCESS", new BigDecimal("150000.00"));

        PaymentTransactionDto dto = new PaymentTransactionDto(
                txId, orderId, "GW-999", new BigDecimal("150000.00"), "SUCCEEDED", Instant.now(), Instant.now()
        );

        when(paymentService.handleCallback(any(PaymentCallbackRequest.class), eq(validSecret))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/payments/callback")
                        .header("X-Webhook-Secret", validSecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.provider_reference").value("GW-999"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/callback: invalid webhook secret returns 401 Unauthorized")
    void testHandleCallback_InvalidSecret() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentCallbackRequest request = new PaymentCallbackRequest(
                orderId, "GW-999", "SUCCESS", new BigDecimal("150000.00"));

        when(paymentService.handleCallback(any(PaymentCallbackRequest.class), eq("wrong-secret")))
                .thenThrow(new AuthenticationFailedException("Invalid or missing webhook signature/secret"));

        mockMvc.perform(post("/api/v1/payments/callback")
                        .header("X-Webhook-Secret", "wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication Failed"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/callback: duplicate payment returns 422 with ruleId BR-002")
    void testHandleCallback_DuplicateViolation() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentCallbackRequest request = new PaymentCallbackRequest(
                orderId, "GW-999", "SUCCESS", new BigDecimal("150000.00"));

        when(paymentService.handleCallback(any(PaymentCallbackRequest.class), any()))
                .thenThrow(new BusinessRuleException("BR-002", "Order already has a succeeded payment. Duplicate payment rejected."));

        mockMvc.perform(post("/api/v1/payments/callback")
                        .header("X-Webhook-Secret", validSecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violated_rule").value("BR-002"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/callback: late callback on expired order returns 422 with ruleId BR-001")
    void testHandleCallback_LateCallback() throws Exception {
        UUID orderId = UUID.randomUUID();
        PaymentCallbackRequest request = new PaymentCallbackRequest(
                orderId, "GW-999", "SUCCESS", new BigDecimal("150000.00"));

        when(paymentService.handleCallback(any(PaymentCallbackRequest.class), any()))
                .thenThrow(new BusinessRuleException("BR-001", "Order is in terminal state EXPIRED. Late payment callback cannot be applied."));

        mockMvc.perform(post("/api/v1/payments/callback")
                        .header("X-Webhook-Secret", validSecret)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violated_rule").value("BR-001"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/timeout-check: returns 200 OK with processed count")
    void testTimeoutCheck() throws Exception {
        when(paymentService.checkTimeouts(15)).thenReturn(3);

        mockMvc.perform(post("/api/v1/payments/timeout-check")
                        .param("timeoutMinutes", "15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(3))
                .andExpect(jsonPath("$.timeoutMinutes").value(15));
    }
}
