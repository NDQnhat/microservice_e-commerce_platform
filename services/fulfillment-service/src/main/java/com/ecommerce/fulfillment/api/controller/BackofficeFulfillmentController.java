package com.ecommerce.fulfillment.api.controller;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.fulfillment.api.dto.ShipmentDto;
import com.ecommerce.fulfillment.api.dto.UpdateShipmentRequest;
import com.ecommerce.fulfillment.domain.model.OutboxEventRecord;
import com.ecommerce.fulfillment.domain.model.Shipment;
import com.ecommerce.fulfillment.domain.model.ShipmentStatus;
import com.ecommerce.fulfillment.domain.repository.OutboxEventRepository;
import com.ecommerce.fulfillment.domain.repository.ShipmentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/fulfillment")
public class BackofficeFulfillmentController {

    private final ShipmentRepository shipmentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public BackofficeFulfillmentController(ShipmentRepository shipmentRepository,
                                           OutboxEventRepository outboxEventRepository,
                                           ObjectMapper objectMapper) {
        this.shipmentRepository = shipmentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<Page<ShipmentDto>> listShipments(
            @RequestParam(required = false) ShipmentStatus status,
            Pageable pageable) {

        Page<Shipment> shipments = status != null
                ? shipmentRepository.findByStatus(status, pageable)
                : shipmentRepository.findAll(pageable);

        return ResponseEntity.ok(shipments.map(s -> new ShipmentDto(
                s.getId(),
                s.getOrderId(),
                s.getCarrierName(),
                s.getTrackingCode(),
                s.getStatus().name(),
                s.getPackedAt(),
                s.getShippedAt(),
                s.getDeliveredAt()
        )));
    }

    @PostMapping("/orders/{orderId}")
    @Transactional
    public ResponseEntity<ShipmentDto> updateShipment(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateShipmentRequest request) {

        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseGet(() -> shipmentRepository.save(new Shipment(orderId)));

        if (!shipment.getStatus().canTransitionTo(request.getTargetStatus())) {
            throw new BusinessRuleException("BR-011",
                    String.format("Invalid shipment transition from %s to %s", shipment.getStatus(), request.getTargetStatus()));
        }

        // BR-011: Carrier and tracking code required before reaching SHIPPED
        if (request.getTargetStatus() == ShipmentStatus.SHIPPED) {
            if (request.getTrackingCode() == null || request.getTrackingCode().trim().isEmpty() ||
                request.getCarrierName() == null || request.getCarrierName().trim().isEmpty()) {
                throw new BusinessRuleException("BR-011", "Tracking code and carrier name must be provided before SHIPPED state.");
            }
            shipment.setCarrierName(request.getCarrierName());
            shipment.setTrackingCode(request.getTrackingCode());
            shipment.setShippedAt(Instant.now());

            publishOutboxEvent("OrderShipped", shipment);
        } else if (request.getTargetStatus() == ShipmentStatus.DELIVERED) {
            shipment.setDeliveredAt(Instant.now());
            publishOutboxEvent("OrderDelivered", shipment);
        }

        shipment.setStatus(request.getTargetStatus());
        Shipment saved = shipmentRepository.save(shipment);

        return ResponseEntity.ok(new ShipmentDto(
                saved.getId(),
                saved.getOrderId(),
                saved.getCarrierName(),
                saved.getTrackingCode(),
                saved.getStatus().name(),
                saved.getPackedAt(),
                saved.getShippedAt(),
                saved.getDeliveredAt()
        ));
    }

    private void publishOutboxEvent(String eventType, Shipment shipment) {
        try {
            String payloadJson = objectMapper.writeValueAsString(shipment.getOrderId());
            OutboxEventRecord outbox = new OutboxEventRecord(
                    "Shipment",
                    shipment.getId().toString(),
                    eventType,
                    payloadJson,
                    CorrelationContext.getCorrelationId()
            );
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize fulfillment event: " + eventType, e);
        }
    }
}
