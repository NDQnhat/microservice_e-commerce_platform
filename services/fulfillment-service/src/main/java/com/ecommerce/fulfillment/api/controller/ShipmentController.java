package com.ecommerce.fulfillment.api.controller;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.fulfillment.api.dto.ShipmentDto;
import com.ecommerce.fulfillment.domain.model.Shipment;
import com.ecommerce.fulfillment.domain.repository.ShipmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipments")
public class ShipmentController {

    private final ShipmentRepository shipmentRepository;

    public ShipmentController(ShipmentRepository shipmentRepository) {
        this.shipmentRepository = shipmentRepository;
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ShipmentDto> getShipmentByOrder(@PathVariable UUID orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Shipment not found for order: " + orderId));

        return ResponseEntity.ok(new ShipmentDto(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getCarrierName(),
                shipment.getTrackingCode(),
                shipment.getStatus().name(),
                shipment.getPackedAt(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt()
        ));
    }
}
