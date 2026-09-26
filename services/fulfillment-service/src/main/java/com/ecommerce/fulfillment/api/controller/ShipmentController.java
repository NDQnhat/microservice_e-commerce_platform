package com.ecommerce.fulfillment.api.controller;

import com.ecommerce.fulfillment.api.dto.ShipmentDto;
import com.ecommerce.fulfillment.service.FulfillmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private final FulfillmentService fulfillmentService;

    public ShipmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ShipmentDto> getShipmentByOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(fulfillmentService.getShipmentByOrderId(orderId));
    }
}
