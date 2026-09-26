package com.ecommerce.fulfillment.api.controller;

import com.ecommerce.fulfillment.api.dto.ShipmentDto;
import com.ecommerce.fulfillment.api.dto.UpdateShipmentRequest;
import com.ecommerce.fulfillment.domain.model.ShipmentStatus;
import com.ecommerce.fulfillment.service.FulfillmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR', 'WAREHOUSE_STAFF', 'SUPER_ADMIN', 'OPS_ADMIN', 'ORDER_OPERATIONS_ADMIN')")
public class BackofficeFulfillmentController {

    private final FulfillmentService fulfillmentService;

    public BackofficeFulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    // ==========================================
    // API-FUL-002: Fulfillment Monitoring View (FR-021, FR-042)
    // ==========================================
    @GetMapping("/fulfillment")
    public ResponseEntity<Page<ShipmentDto>> listShipments(
            @RequestParam(required = false) ShipmentStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(fulfillmentService.listShipments(status, pageable));
    }

    // ==========================================
    // FR-042: Stuck Shipments Monitoring View
    // ==========================================
    @GetMapping({"/fulfillment/stuck", "/orders/stuck-fulfillment"})
    public ResponseEntity<List<ShipmentDto>> getStuckShipments(
            @RequestParam(defaultValue = "60") int thresholdMinutes) {
        return ResponseEntity.ok(fulfillmentService.getStuckShipments(thresholdMinutes));
    }

    // ==========================================
    // SHP-T01: Initiate Shipment (PACKING)
    // ==========================================
    @PostMapping({"/fulfillment/orders/{orderId}/initiate", "/orders/{orderId}/initiate-shipment"})
    public ResponseEntity<ShipmentDto> initiateShipment(@PathVariable UUID orderId) {
        return ResponseEntity.ok(fulfillmentService.initiateShipment(orderId));
    }

    // ==========================================
    // API-FUL-001: Update Shipment Info / Transition (FR-040, BR-011)
    // Supports both /backoffice/orders/{orderId}/shipment and /backoffice/fulfillment/orders/{orderId}
    // ==========================================
    @PostMapping({
            "/orders/{orderId}/shipment",
            "/fulfillment/orders/{orderId}",
            "/fulfillment/orders/{orderId}/shipment"
    })
    public ResponseEntity<ShipmentDto> updateShipment(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateShipmentRequest request) {
        return ResponseEntity.ok(fulfillmentService.updateShipment(orderId, request));
    }
}
