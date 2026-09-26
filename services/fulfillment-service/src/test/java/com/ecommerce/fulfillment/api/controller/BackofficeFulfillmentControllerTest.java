package com.ecommerce.fulfillment.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.fulfillment.api.dto.ShipmentDto;
import com.ecommerce.fulfillment.api.dto.UpdateShipmentRequest;
import com.ecommerce.fulfillment.domain.model.ShipmentStatus;
import com.ecommerce.fulfillment.service.FulfillmentService;
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
class BackofficeFulfillmentControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FulfillmentService fulfillmentService;

    @InjectMocks
    private BackofficeFulfillmentController backofficeFulfillmentController;

    private UUID orderId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(backofficeFulfillmentController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/fulfillment: returns 200 with Page of ShipmentDto (API-FUL-002)")
    void listShipments_returns200() throws Exception {
        ShipmentDto dto = new ShipmentDto(UUID.randomUUID(), orderId, "VNPost", "VNP123",
                "PACKING", Instant.now(), null, null);
        when(fulfillmentService.listShipments(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), org.springframework.data.domain.PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/backoffice/fulfillment")
                        .param("status", "PACKING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].order_id").value(orderId.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PACKING"));
    }

    @Test
    @DisplayName("GET /api/v1/backoffice/fulfillment/stuck: returns 200 with stuck shipments list (FR-042)")
    void getStuckShipments_returns200() throws Exception {
        ShipmentDto dto = new ShipmentDto(UUID.randomUUID(), orderId, null, null,
                "PACKING", Instant.now().minusSeconds(7200), null, null);
        when(fulfillmentService.getStuckShipments(60))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/backoffice/fulfillment/stuck")
                        .param("thresholdMinutes", "60")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].order_id").value(orderId.toString()))
                .andExpect(jsonPath("$[0].status").value("PACKING"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/fulfillment/orders/{orderId}/initiate: returns 200 (SHP-T01)")
    void initiateShipment_returns200() throws Exception {
        ShipmentDto dto = new ShipmentDto(UUID.randomUUID(), orderId, null, null,
                "PACKING", Instant.now(), null, null);
        when(fulfillmentService.initiateShipment(orderId)).thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/fulfillment/orders/{orderId}/initiate", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PACKING"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/orders/{orderId}/shipment: valid SHIPPED transition returns 200 (API-FUL-001)")
    void updateShipment_validShipped_returns200() throws Exception {
        UpdateShipmentRequest request = new UpdateShipmentRequest("VNPost", "VNP123456", ShipmentStatus.SHIPPED);
        ShipmentDto dto = new ShipmentDto(UUID.randomUUID(), orderId, "VNPost", "VNP123456",
                "SHIPPED", Instant.now(), Instant.now(), null);

        when(fulfillmentService.updateShipment(eq(orderId), any(UpdateShipmentRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/orders/{orderId}/shipment", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carrier_name").value("VNPost"))
                .andExpect(jsonPath("$.tracking_code").value("VNP123456"))
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/fulfillment/orders/{orderId}: alias path returns 200")
    void updateShipment_aliasPath_returns200() throws Exception {
        UpdateShipmentRequest request = new UpdateShipmentRequest("VNPost", "VNP123456", ShipmentStatus.SHIPPED);
        ShipmentDto dto = new ShipmentDto(UUID.randomUUID(), orderId, "VNPost", "VNP123456",
                "SHIPPED", Instant.now(), Instant.now(), null);

        when(fulfillmentService.updateShipment(eq(orderId), any(UpdateShipmentRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/v1/backoffice/fulfillment/orders/{orderId}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/orders/{orderId}/shipment: missing target_status returns 400 Bad Request")
    void updateShipment_missingTargetStatus_returns400() throws Exception {
        String invalidJson = "{\"carrier_name\": \"VNPost\"}";

        mockMvc.perform(post("/api/v1/backoffice/orders/{orderId}/shipment", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/backoffice/orders/{orderId}/shipment: BR-011 violation returns 422 RFC 7807")
    void updateShipment_br011Violation_returns422() throws Exception {
        UpdateShipmentRequest request = new UpdateShipmentRequest(null, null, ShipmentStatus.SHIPPED);
        when(fulfillmentService.updateShipment(eq(orderId), any(UpdateShipmentRequest.class)))
                .thenThrow(new BusinessRuleException("BR-011", "Tracking code and carrier name must be provided before SHIPPED state."));

        mockMvc.perform(post("/api/v1/backoffice/orders/{orderId}/shipment", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violated_rule").value("BR-011"))
                .andExpect(jsonPath("$.detail").value("[BR-011] Tracking code and carrier name must be provided before SHIPPED state."));
    }
}
