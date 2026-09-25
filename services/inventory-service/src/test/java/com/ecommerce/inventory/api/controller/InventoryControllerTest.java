package com.ecommerce.inventory.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.InvalidStateException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.inventory.api.dto.*;
import com.ecommerce.inventory.service.InventoryService;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    private UUID skuId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        skuId = UUID.randomUUID();
        orderId = UUID.randomUUID();
    }

    // ==========================================
    // API-INV-001: GET /api/v1/inventory/{skuId}
    // ==========================================

    @Test
    @DisplayName("API-INV-001 Happy Path: GET /api/v1/inventory/{skuId} returns 200 with stock and warehouse breakdown")
    void getStock_HappyPath() throws Exception {
        InventoryDto dto = new InventoryDto(skuId, 100, 20, 80);
        when(inventoryService.getInventory(skuId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/" + skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku_id").value(skuId.toString()))
                .andExpect(jsonPath("$.quantity_on_hand").value(100))
                .andExpect(jsonPath("$.quantity_reserved").value(20))
                .andExpect(jsonPath("$.quantity_available").value(80))
                .andExpect(jsonPath("$.warehouses").isArray());
    }

    @Test
    @DisplayName("API-INV-001 Alias: GET /api/v1/inventory/skus/{skuId} returns 200")
    void getStock_AliasPath() throws Exception {
        InventoryDto dto = new InventoryDto(skuId, 50, 0, 50);
        when(inventoryService.getInventory(skuId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/inventory/skus/" + skuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku_id").value(skuId.toString()));
    }

    @Test
    @DisplayName("API-INV-001 Not Found: GET /api/v1/inventory/{skuId} with unknown SKU returns 404")
    void getStock_NotFound() throws Exception {
        when(inventoryService.getInventory(skuId))
                .thenThrow(new NotFoundException("Inventory record not found for SKU: " + skuId));

        mockMvc.perform(get("/api/v1/inventory/" + skuId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // ==========================================
    // API-INV-002: POST /api/v1/inventory/reserve
    // ==========================================

    @Test
    @DisplayName("API-INV-002 Happy Path: POST /api/v1/inventory/reserve returns 201 Created")
    void reserveStock_HappyPath() throws Exception {
        ReserveStockRequest request = new ReserveStockRequest(orderId, List.of(
                new ReservationItemRequest(skuId, 5)
        ));
        UUID resId = UUID.randomUUID();
        ReserveStockResponse response = new ReserveStockResponse(orderId, List.of(resId), "ACTIVE", Instant.now());

        when(inventoryService.reserveStock(any(ReserveStockRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order_id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.reservation_ids[0]").value(resId.toString()));
    }

    @Test
    @DisplayName("API-INV-002 Alias: POST /api/v1/inventory/reservations returns 201 Created")
    void reserveStock_AliasPath() throws Exception {
        ReserveStockRequest request = new ReserveStockRequest(orderId, List.of(
                new ReservationItemRequest(skuId, 2)
        ));
        ReserveStockResponse response = new ReserveStockResponse(orderId, List.of(UUID.randomUUID()), "ACTIVE", Instant.now());

        when(inventoryService.reserveStock(any(ReserveStockRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("API-INV-002 BR-004 Violation: POST /api/v1/inventory/reserve returns 422 Unprocessable Entity")
    void reserveStock_BR004_InsufficientStock() throws Exception {
        ReserveStockRequest request = new ReserveStockRequest(orderId, List.of(
                new ReservationItemRequest(skuId, 1000)
        ));

        when(inventoryService.reserveStock(any(ReserveStockRequest.class)))
                .thenThrow(new BusinessRuleException("BR-004", "[BR-004] Insufficient available inventory for SKU: " + skuId));

        mockMvc.perform(post("/api/v1/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem-type:business-rule-violation"))
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violated_rule").value("BR-004"));
    }

    @Test
    @DisplayName("API-INV-002 Validation Error: POST /api/v1/inventory/reserve with empty items returns 400")
    void reserveStock_ValidationError() throws Exception {
        String invalidJson = "{\"order_id\": \"" + orderId + "\", \"items\": []}";

        mockMvc.perform(post("/api/v1/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ==========================================
    // API-INV-003: POST /api/v1/inventory/release
    // ==========================================

    @Test
    @DisplayName("API-INV-003 Happy Path: POST /api/v1/inventory/release returns 200 with RELEASED status")
    void releaseStock_HappyPath() throws Exception {
        ReleaseStockRequest request = new ReleaseStockRequest(orderId, "ORDER_CANCELLED");
        ReleaseStockResponse response = new ReleaseStockResponse(orderId, "RELEASED");

        when(inventoryService.releaseStock(eq(orderId), eq("ORDER_CANCELLED"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("RELEASED"));
    }

    @Test
    @DisplayName("API-INV-003 Alias: POST /api/v1/inventory/reservations/orders/{orderId}/release returns 200")
    void releaseStock_AliasPath() throws Exception {
        ReleaseStockResponse response = new ReleaseStockResponse(orderId, "RELEASED");
        when(inventoryService.releaseStock(eq(orderId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/reservations/orders/" + orderId + "/release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"));
    }

    @Test
    @DisplayName("API-INV-003 Forbidden Transition: POST /api/v1/inventory/release on consumed reservation returns 409")
    void releaseStock_ForbiddenTransition() throws Exception {
        when(inventoryService.releaseStock(eq(orderId), any()))
                .thenThrow(new InvalidStateException("Cannot release already CONSUMED reservation"));

        mockMvc.perform(post("/api/v1/inventory/reservations/orders/" + orderId + "/release"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    // ==========================================
    // API-INV-004: POST /api/v1/inventory/commit
    // ==========================================

    @Test
    @DisplayName("API-INV-004 Happy Path: POST /api/v1/inventory/commit returns 200 with CONSUMED status")
    void commitStock_HappyPath() throws Exception {
        CommitStockRequest request = new CommitStockRequest(orderId);
        CommitStockResponse response = new CommitStockResponse(orderId, "CONSUMED");

        when(inventoryService.commitStock(eq(orderId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CONSUMED"));
    }

    @Test
    @DisplayName("API-INV-004 Alias: POST /api/v1/inventory/reservations/orders/{orderId}/consume returns 200")
    void commitStock_AliasPath() throws Exception {
        CommitStockResponse response = new CommitStockResponse(orderId, "CONSUMED");
        when(inventoryService.commitStock(eq(orderId))).thenReturn(response);

        mockMvc.perform(post("/api/v1/inventory/reservations/orders/" + orderId + "/consume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONSUMED"));
    }

    @Test
    @DisplayName("API-INV-004 Forbidden Transition: POST /api/v1/inventory/commit on terminal released returns 409")
    void commitStock_ForbiddenTransition() throws Exception {
        when(inventoryService.commitStock(eq(orderId)))
                .thenThrow(new InvalidStateException("Cannot commit reservation in terminal state: RELEASED"));

        mockMvc.perform(post("/api/v1/inventory/reservations/orders/" + orderId + "/consume"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }
}
