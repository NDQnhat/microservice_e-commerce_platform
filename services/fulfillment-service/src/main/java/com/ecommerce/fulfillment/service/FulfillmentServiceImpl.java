package com.ecommerce.fulfillment.service;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.fulfillment.api.dto.ShipmentDto;
import com.ecommerce.fulfillment.api.dto.UpdateShipmentRequest;
import com.ecommerce.fulfillment.client.OrderClient;
import com.ecommerce.fulfillment.domain.event.OrderDeliveredEvent;
import com.ecommerce.fulfillment.domain.event.OrderShippedEvent;
import com.ecommerce.fulfillment.domain.model.Shipment;
import com.ecommerce.fulfillment.domain.model.ShipmentStatus;
import com.ecommerce.fulfillment.domain.repository.ShipmentRepository;
import com.ecommerce.fulfillment.domain.statemachine.ShipmentStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FulfillmentServiceImpl implements FulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentServiceImpl.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentStateMachine stateMachine;
    private final OrderClient orderClient;
    private final FulfillmentOutboxService outboxService;

    public FulfillmentServiceImpl(ShipmentRepository shipmentRepository,
                                  ShipmentStateMachine stateMachine,
                                  OrderClient orderClient,
                                  FulfillmentOutboxService outboxService) {
        this.shipmentRepository = shipmentRepository;
        this.stateMachine = stateMachine;
        this.orderClient = orderClient;
        this.outboxService = outboxService;
    }

    @Override
    @Transactional
    public ShipmentDto initiateShipment(UUID orderId) {
        Optional<Shipment> existing = shipmentRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            log.info("Shipment already exists for order {}. Returning existing record.", orderId);
            return toDto(existing.get());
        }

        String orderStatus = orderClient.getOrderStatus(orderId);
        stateMachine.validateTransition(null, ShipmentStatus.PACKING, null, null, orderStatus);

        Shipment shipment = new Shipment(orderId);
        Shipment saved = shipmentRepository.save(shipment);

        // Drive ORD-T07 on order-service
        orderClient.updateOrderStatus(orderId, "PACKING", "Fulfillment initiated: order in PACKING state");

        log.info("Initiated shipment {} for order {}", saved.getId(), orderId);
        return toDto(saved);
    }

    @Override
    @Transactional
    public ShipmentDto updateShipment(UUID orderId, UpdateShipmentRequest request) {
        if (request == null || request.getTargetStatus() == null) {
            throw new IllegalArgumentException("Target status is required");
        }

        Optional<Shipment> shipmentOpt = shipmentRepository.findByOrderId(orderId);

        if (shipmentOpt.isEmpty()) {
            if (request.getTargetStatus() == ShipmentStatus.PACKING) {
                return initiateShipment(orderId);
            }
            if (request.getTargetStatus() == ShipmentStatus.SHIPPED) {
                String orderStatus = orderClient.getOrderStatus(orderId);
                stateMachine.validateTransition(null, ShipmentStatus.PACKING, null, null, orderStatus);
                stateMachine.validateTransition(ShipmentStatus.PACKING, ShipmentStatus.SHIPPED,
                        request.getCarrierName(), request.getTrackingCode(), "PACKING");

                Shipment shipment = new Shipment(orderId);
                shipment.setCarrierName(request.getCarrierName().trim());
                shipment.setTrackingCode(request.getTrackingCode().trim());
                shipment.setStatus(ShipmentStatus.SHIPPED);
                shipment.setShippedAt(Instant.now());
                Shipment saved = shipmentRepository.save(shipment);

                // Publish outbox event
                OrderShippedEvent event = new OrderShippedEvent(
                        saved.getId(), saved.getOrderId(), saved.getCarrierName(), saved.getTrackingCode(), saved.getShippedAt()
                );
                outboxService.recordEvent(saved.getId().toString(), "OrderShipped", event);

                // Drive ORD-T07 then ORD-T08
                orderClient.updateOrderStatus(orderId, "PACKING", "Fulfillment initiated");
                orderClient.updateOrderStatus(orderId, "SHIPPED", "Shipment dispatched via " + saved.getCarrierName());

                return toDto(saved);
            }
            if (request.getTargetStatus() == ShipmentStatus.DELIVERED) {
                throw new BusinessRuleException("BR-011", "Direct transition from PACKING/none to DELIVERED is forbidden.");
            }
        }

        Shipment shipment = shipmentOpt.get();

        // Idempotency check: if status is already target
        if (shipment.getStatus() == request.getTargetStatus()) {
            if (request.getTargetStatus() == ShipmentStatus.SHIPPED) {
                boolean updated = false;
                if (request.getCarrierName() != null && !request.getCarrierName().isBlank()) {
                    shipment.setCarrierName(request.getCarrierName().trim());
                    updated = true;
                }
                if (request.getTrackingCode() != null && !request.getTrackingCode().isBlank()) {
                    shipment.setTrackingCode(request.getTrackingCode().trim());
                    updated = true;
                }
                if (updated) {
                    return toDto(shipmentRepository.save(shipment));
                }
            }
            return toDto(shipment);
        }

        String orderStatus = orderClient.getOrderStatus(orderId);
        stateMachine.validateTransition(shipment.getStatus(), request.getTargetStatus(),
                request.getCarrierName(), request.getTrackingCode(), orderStatus);

        if (request.getTargetStatus() == ShipmentStatus.SHIPPED) {
            shipment.setCarrierName(request.getCarrierName().trim());
            shipment.setTrackingCode(request.getTrackingCode().trim());
            shipment.setStatus(ShipmentStatus.SHIPPED);
            shipment.setShippedAt(Instant.now());
            Shipment saved = shipmentRepository.save(shipment);

            // Record outbox event
            OrderShippedEvent event = new OrderShippedEvent(
                    saved.getId(), saved.getOrderId(), saved.getCarrierName(), saved.getTrackingCode(), saved.getShippedAt()
            );
            outboxService.recordEvent(saved.getId().toString(), "OrderShipped", event);

            // Drive ORD-T08 on order-service
            orderClient.updateOrderStatus(orderId, "SHIPPED",
                    "Dispatched via " + saved.getCarrierName() + " (tracking: " + saved.getTrackingCode() + ")");

            return toDto(saved);
        } else if (request.getTargetStatus() == ShipmentStatus.DELIVERED) {
            shipment.setStatus(ShipmentStatus.DELIVERED);
            shipment.setDeliveredAt(Instant.now());
            Shipment saved = shipmentRepository.save(shipment);

            // Record outbox event
            OrderDeliveredEvent event = new OrderDeliveredEvent(
                    saved.getId(), saved.getOrderId(), saved.getCarrierName(), saved.getTrackingCode(), saved.getDeliveredAt()
            );
            outboxService.recordEvent(saved.getId().toString(), "OrderDelivered", event);

            // Drive ORD-T09 on order-service (SHIPPED -> COMPLETED)
            orderClient.updateOrderStatus(orderId, "COMPLETED", "Delivery confirmed by courier");

            return toDto(saved);
        }

        return toDto(shipmentRepository.save(shipment));
    }

    @Override
    @Transactional(readOnly = true)
    public ShipmentDto getShipmentByOrderId(UUID orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException("Shipment not found for order: " + orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShipmentDto> listShipments(ShipmentStatus status, Pageable pageable) {
        Page<Shipment> page = status != null
                ? shipmentRepository.findByStatus(status, pageable)
                : shipmentRepository.findAll(pageable);
        return page.map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShipmentDto> getStuckShipments(int thresholdMinutes) {
        Instant cutoff = Instant.now().minus(thresholdMinutes, ChronoUnit.MINUTES);
        return shipmentRepository.findByStatusAndPackedAtBefore(ShipmentStatus.PACKING, cutoff)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ShipmentDto toDto(Shipment shipment) {
        return new ShipmentDto(
                shipment.getId(),
                shipment.getOrderId(),
                shipment.getCarrierName(),
                shipment.getTrackingCode(),
                shipment.getStatus().name(),
                shipment.getPackedAt(),
                shipment.getShippedAt(),
                shipment.getDeliveredAt()
        );
    }
}
