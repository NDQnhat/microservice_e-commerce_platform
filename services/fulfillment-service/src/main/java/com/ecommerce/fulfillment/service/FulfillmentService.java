package com.ecommerce.fulfillment.service;

import com.ecommerce.fulfillment.api.dto.ShipmentDto;
import com.ecommerce.fulfillment.api.dto.UpdateShipmentRequest;
import com.ecommerce.fulfillment.domain.model.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface FulfillmentService {

    /**
     * SHP-T01: Initiates shipment for an order (sets status to PACKING).
     * Precondition: Order must be in PAID state. Drives ORD-T07.
     */
    ShipmentDto initiateShipment(UUID orderId);

    /**
     * SHP-T02 / SHP-T03: Updates shipment details and drives state transitions.
     * Enforces carrier/tracking guards (BR-011) and synchronizes with order-service (ORD-T08, ORD-T09).
     */
    ShipmentDto updateShipment(UUID orderId, UpdateShipmentRequest request);

    /**
     * Retrieves shipment details by orderId.
     */
    ShipmentDto getShipmentByOrderId(UUID orderId);

    /**
     * Lists shipments with optional status filter and pagination (API-FUL-002, FR-021).
     */
    Page<ShipmentDto> listShipments(ShipmentStatus status, Pageable pageable);

    /**
     * FR-042: Retrieves shipments stuck in PACKING status longer than threshold minutes.
     */
    List<ShipmentDto> getStuckShipments(int thresholdMinutes);
}
