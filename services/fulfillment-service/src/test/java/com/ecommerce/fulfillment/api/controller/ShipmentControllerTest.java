package com.ecommerce.fulfillment.api.controller;

import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.fulfillment.api.dto.ShipmentDto;
import com.ecommerce.fulfillment.service.FulfillmentService;
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
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ShipmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FulfillmentService fulfillmentService;

    @InjectMocks
    private ShipmentController shipmentController;

    private UUID orderId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(shipmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        orderId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/v1/shipments/orders/{orderId}: returns 200 with shipment info")
    void getShipmentByOrder_returns200() throws Exception {
        ShipmentDto dto = new ShipmentDto(UUID.randomUUID(), orderId, "VNPost", "VNP123456",
                "SHIPPED", Instant.now(), Instant.now(), null);

        when(fulfillmentService.getShipmentByOrderId(orderId)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/shipments/orders/{orderId}", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value(orderId.toString()))
                .andExpect(jsonPath("$.carrier_name").value("VNPost"))
                .andExpect(jsonPath("$.tracking_code").value("VNP123456"))
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    @DisplayName("GET /api/v1/shipments/orders/{orderId}: returns 404 when shipment not found")
    void getShipmentByOrder_notFound_returns404() throws Exception {
        when(fulfillmentService.getShipmentByOrderId(orderId))
                .thenThrow(new NotFoundException("Shipment not found for order: " + orderId));

        mockMvc.perform(get("/api/v1/shipments/orders/{orderId}", orderId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
