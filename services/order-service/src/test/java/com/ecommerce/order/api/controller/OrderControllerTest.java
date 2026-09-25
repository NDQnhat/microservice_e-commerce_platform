package com.ecommerce.order.api.controller;

import com.ecommerce.common.error.AuthorizationFailedException;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.order.api.dto.CancelOrderRequest;
import com.ecommerce.order.api.dto.CreateOrderRequest;
import com.ecommerce.order.api.dto.OrderItemResponse;
import com.ecommerce.order.api.dto.OrderResponse;
import com.ecommerce.order.security.UserPrincipal;
import com.ecommerce.order.service.CreateOrderResult;
import com.ecommerce.order.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private UUID customerId;
    private UUID orderId;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        idempotencyKey = "key-" + UUID.randomUUID();

        UserPrincipal principal = new UserPrincipal(customerId, "customer@example.com", List.of("CUSTOMER"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==========================================
    // API-ORD-001: POST /api/v1/orders
    // ==========================================

    @Test
    @DisplayName("API-ORD-001: Missing Idempotency-Key header returns 400 Bad Request")
    void createOrder_MissingIdempotencyKey_Returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API-ORD-001: Successful checkout returns 201 Created with order response")
    void createOrder_Success_Returns201() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);

        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setId(orderId);
        orderResponse.setCustomerId(customerId);
        orderResponse.setStatus("RESERVED");
        orderResponse.setGrandTotalAmount(new BigDecimal("500000.00"));

        when(orderService.createOrder(eq(customerId), eq(idempotencyKey), any(CreateOrderRequest.class)))
                .thenReturn(new CreateOrderResult(orderResponse, false));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.customer_id").value(customerId.toString()))
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.grand_total_amount").value(500000.00));
    }

    @Test
    @DisplayName("API-ORD-001 / BR-010: Idempotent replay returns 200 OK with original order")
    void createOrder_IdempotentReplay_Returns200() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);

        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setId(orderId);
        orderResponse.setCustomerId(customerId);
        orderResponse.setStatus("RESERVED");

        when(orderService.createOrder(eq(customerId), eq(idempotencyKey), any(CreateOrderRequest.class)))
                .thenReturn(new CreateOrderResult(orderResponse, true));

        mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    // ==========================================
    // API-ORD-002: GET /api/v1/orders/{orderId}
    // ==========================================

    @Test
    @DisplayName("API-ORD-002: Customer gets own order returns 200 OK")
    void getOrder_Success_Returns200() throws Exception {
        OrderResponse orderResponse = new OrderResponse();
        orderResponse.setId(orderId);
        orderResponse.setCustomerId(customerId);
        orderResponse.setStatus("RESERVED");
        orderResponse.setItems(Collections.emptyList());
        orderResponse.setTimeline(Collections.emptyList());

        when(orderService.getOrder(eq(orderId), eq(customerId), anyBoolean()))
                .thenReturn(orderResponse);

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("RESERVED"));
    }

    @Test
    @DisplayName("API-ORD-002: Customer accessing unauthorized order returns 403 Forbidden")
    void getOrder_Unauthorized_Returns403() throws Exception {
        when(orderService.getOrder(eq(orderId), eq(customerId), anyBoolean()))
                .thenThrow(new AuthorizationFailedException("Access denied"));

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_FAILED"));
    }

    @Test
    @DisplayName("API-ORD-002: Order not found returns 404 Not Found")
    void getOrder_NotFound_Returns404() throws Exception {
        when(orderService.getOrder(eq(orderId), eq(customerId), anyBoolean()))
                .thenThrow(new NotFoundException("Order not found: " + orderId));

        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ==========================================
    // API-ORD-003: POST /api/v1/orders/{orderId}/cancel
    // ==========================================

    @Test
    @DisplayName("API-ORD-003: Cancellation before cutoff returns 200 OK with CANCELLED status")
    void cancelOrder_BeforeCutoff_Returns200() throws Exception {
        CancelOrderRequest request = new CancelOrderRequest("Customer request");

        OrderResponse cancelledResponse = new OrderResponse();
        cancelledResponse.setId(orderId);
        cancelledResponse.setStatus("CANCELLED");

        when(orderService.cancelOrder(eq(orderId), eq(customerId), anyBoolean(), any(CancelOrderRequest.class)))
                .thenReturn(cancelledResponse);

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("API-ORD-003 / BR-006: Cancellation attempt after cutoff returns 422 Business Rule Violation")
    void cancelOrder_AfterCutoff_Returns422() throws Exception {
        CancelOrderRequest request = new CancelOrderRequest("Customer request");

        when(orderService.cancelOrder(eq(orderId), eq(customerId), anyBoolean(), any(CancelOrderRequest.class)))
                .thenThrow(new BusinessRuleException("BR-006", "Cancellation cutoff exceeded; order is already in PACKING state."));

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violated_rule").value("BR-006"));
    }
}
