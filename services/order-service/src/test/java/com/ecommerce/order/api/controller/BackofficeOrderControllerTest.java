package com.ecommerce.order.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.order.api.dto.OrderResponse;
import com.ecommerce.order.api.dto.OrderTransitionRequest;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.security.UserPrincipal;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BackofficeOrderControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private OrderService orderService;

    @InjectMocks
    private BackofficeOrderController backofficeOrderController;

    private UUID orderId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(backofficeOrderController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        orderId = UUID.randomUUID();

        UserPrincipal admin = new UserPrincipal(UUID.randomUUID(), "admin@example.com", List.of("ORDER_OPERATIONS_ADMIN"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("API-ORD-004: GET /api/v1/backoffice/orders returns 200 with paged orders")
    void listOrders_Success_Returns200() throws Exception {
        OrderResponse response = new OrderResponse();
        response.setId(orderId);
        response.setStatus("RESERVED");

        Page<OrderResponse> page = new PageImpl<>(List.of(response), org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(orderService.listOrders(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/backoffice/orders").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("RESERVED"));
    }

    @Test
    @DisplayName("API-ORD-005: PUT /api/v1/backoffice/orders/{orderId}/status updates status and returns 200")
    void updateOrderStatus_Success_Returns200() throws Exception {
        OrderResponse updated = new OrderResponse();
        updated.setId(orderId);
        updated.setStatus("PAID");

        when(orderService.transitionOrderStatus(eq(orderId), any(OrderTransitionRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/backoffice/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("target_status", "PAID", "note", "Payment confirmed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("API-ORD-005 / BR-011: Invalid transition returns 422 Business Rule Violation")
    void updateOrderStatus_InvalidTransition_Returns422() throws Exception {
        when(orderService.transitionOrderStatus(eq(orderId), any(OrderTransitionRequest.class)))
                .thenThrow(new BusinessRuleException("BR-007", "Invalid order state transition"));

        mockMvc.perform(put("/api/v1/backoffice/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("target_status", "COMPLETED"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violated_rule").value("BR-007"));
    }

    @Test
    @DisplayName("API-ORD-005: POST /api/v1/backoffice/orders/{orderId}/transition advances status successfully")
    void transitionOrder_Success_Returns200() throws Exception {
        OrderResponse updated = new OrderResponse();
        updated.setId(orderId);
        updated.setStatus("PACKING");

        when(orderService.transitionOrderStatus(eq(orderId), any(OrderTransitionRequest.class)))
                .thenReturn(updated);

        OrderTransitionRequest request = new OrderTransitionRequest(OrderStatus.PACKING, UUID.randomUUID(), "Packing");

        mockMvc.perform(post("/api/v1/backoffice/orders/{orderId}/transition", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PACKING"));
    }

    @Test
    @DisplayName("API-ORD-006: GET /api/v1/backoffice/orders/stuck returns 200 with stuck orders")
    void getStuckOrders_Success_Returns200() throws Exception {
        OrderResponse stuck = new OrderResponse();
        stuck.setId(orderId);
        stuck.setStatus("RESERVED");

        when(orderService.getStuckOrders(60)).thenReturn(List.of(stuck));

        mockMvc.perform(get("/api/v1/backoffice/orders/stuck").param("thresholdMinutes", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(orderId.toString()))
                .andExpect(jsonPath("$[0].status").value("RESERVED"));
    }

    @Test
    @DisplayName("API-ORD-007: POST /api/v1/backoffice/orders/{orderId}/retry returns 202 Accepted")
    void retryStuckOrder_Success_Returns202() throws Exception {
        doNothing().when(orderService).retryStuckOrder(orderId);

        mockMvc.perform(post("/api/v1/backoffice/orders/{orderId}/retry", orderId))
                .andExpect(status().isAccepted());
    }
}
