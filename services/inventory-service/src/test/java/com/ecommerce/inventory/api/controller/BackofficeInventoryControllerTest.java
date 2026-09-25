package com.ecommerce.inventory.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.inventory.api.dto.AdjustInventoryRequest;
import com.ecommerce.inventory.api.dto.InventoryAdjustmentLogDto;
import com.ecommerce.inventory.api.dto.InventoryDto;
import com.ecommerce.inventory.domain.model.AdjustmentReasonCode;
import com.ecommerce.inventory.service.InventoryAdjustmentService;
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
class BackofficeInventoryControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private InventoryAdjustmentService adjustmentService;

    @InjectMocks
    private BackofficeInventoryController backofficeController;

    private UUID skuId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(backofficeController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        skuId = UUID.randomUUID();
        actorId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Adjust inventory: POST /api/v1/backoffice/skus/{skuId}/inventory/adjustments returns 200")
    void adjustInventory_HappyPath() throws Exception {
        AdjustInventoryRequest request = new AdjustInventoryRequest(20, AdjustmentReasonCode.RESTOCK, "New stock arrived");
        InventoryDto dto = new InventoryDto(skuId, 120, 0, 120);

        when(adjustmentService.adjustInventory(eq(skuId), any(UUID.class), any(AdjustInventoryRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/skus/" + skuId + "/inventory/adjustments")
                        .param("actorId", actorId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku_id").value(skuId.toString()))
                .andExpect(jsonPath("$.quantity_on_hand").value(120))
                .andExpect(jsonPath("$.quantity_available").value(120));
    }

    @Test
    @DisplayName("Adjust inventory: Negative boundary violation returns 422 Business Rule Violation")
    void adjustInventory_BoundaryNegative_Returns422() throws Exception {
        AdjustInventoryRequest request = new AdjustInventoryRequest(-500, AdjustmentReasonCode.CORRECTION, "Discrepancy");

        when(adjustmentService.adjustInventory(eq(skuId), any(UUID.class), any(AdjustInventoryRequest.class)))
                .thenThrow(new BusinessRuleException("BR-004", "[BR-004] Inventory on-hand quantity cannot be negative"));

        mockMvc.perform(post("/api/v1/backoffice/skus/" + skuId + "/inventory/adjustments")
                        .param("actorId", actorId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:problem-type:business-rule-violation"))
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violated_rule").value("BR-004"));
    }

    @Test
    @DisplayName("Audit log: GET /api/v1/backoffice/skus/{skuId}/inventory/audit-log returns 200")
    void getAuditLog_HappyPath() throws Exception {
        InventoryAdjustmentLogDto logDto = new InventoryAdjustmentLogDto(
                UUID.randomUUID(), skuId, actorId, 100, 120, 20, "RESTOCK", "Restock", Instant.now()
        );

        when(adjustmentService.getAuditLog(eq(skuId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(logDto), org.springframework.data.domain.PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/backoffice/skus/" + skuId + "/inventory/audit-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].sku_id").value(skuId.toString()))
                .andExpect(jsonPath("$.content[0].delta").value(20));
    }
}
