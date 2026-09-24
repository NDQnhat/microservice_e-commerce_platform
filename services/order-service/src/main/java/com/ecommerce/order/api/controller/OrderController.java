package com.ecommerce.order.api.controller;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.order.api.dto.*;
import com.ecommerce.order.domain.model.*;
import com.ecommerce.order.domain.repository.OrderItemRepository;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.order.domain.repository.OrderTimelineEventRepository;
import com.ecommerce.order.domain.repository.OutboxEventRepository;
import com.ecommerce.order.domain.statemachine.OrderStateMachine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTimelineEventRepository timelineRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderStateMachine stateMachine;
    private final ObjectMapper objectMapper;

    public OrderController(OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           OrderTimelineEventRepository timelineRepository,
                           OutboxEventRepository outboxEventRepository,
                           OrderStateMachine stateMachine,
                           ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.timelineRepository = timelineRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.stateMachine = stateMachine;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {

        // BR-010: Idempotency replay check
        Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOrder.isPresent()) {
            return ResponseEntity.ok(mapToResponse(existingOrder.get()));
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemRequest item : request.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal grandTotal = subtotal.add(request.getShippingFeeAmount()).subtract(discount);

        Order order = new Order(
                request.getCustomerId(),
                idempotencyKey,
                request.getShippingRecipientName(),
                request.getShippingPhone(),
                request.getShippingLine1(),
                request.getShippingLine2(),
                request.getShippingWard(),
                request.getShippingDistrict(),
                request.getShippingCity(),
                subtotal,
                request.getShippingFeeAmount(),
                discount,
                grandTotal,
                request.getCurrency()
        );

        Order savedOrder = orderRepository.save(order);

        for (OrderItemRequest itemReq : request.getItems()) {
            OrderItem orderItem = new OrderItem(
                    savedOrder,
                    itemReq.getSkuId(),
                    itemReq.getProductName(),
                    itemReq.getSkuCode(),
                    itemReq.getAttributeSnapshot(),
                    itemReq.getUnitPrice(),
                    itemReq.getQuantity()
            );
            orderItemRepository.save(orderItem);
            savedOrder.getItems().add(orderItem);
        }

        // BR-008: Record initial timeline event (null -> RESERVED)
        OrderTimelineEvent initialTimeline = new OrderTimelineEvent(
                savedOrder,
                null,
                OrderStatus.RESERVED,
                request.getCustomerId(),
                TimelineActorType.CUSTOMER,
                "Order created via checkout"
        );
        timelineRepository.save(initialTimeline);
        savedOrder.getTimelineEvents().add(initialTimeline);

        // Transactional Outbox: OrderCreated event
        try {
            String payloadJson = objectMapper.writeValueAsString(savedOrder.getId());
            OutboxEventRecord outbox = new OutboxEventRecord(
                    "Order",
                    savedOrder.getId().toString(),
                    "OrderCreated",
                    payloadJson,
                    CorrelationContext.getCorrelationId()
            );
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize OrderCreated event", e);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(savedOrder));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        return ResponseEntity.ok(mapToResponse(order));
    }

    @PostMapping("/{orderId}/cancel")
    @Transactional
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable UUID orderId,
            @RequestParam UUID customerId,
            @Valid @RequestBody CancelOrderRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        // State Machine & BR-006 validation
        stateMachine.validateTransition(order.getStatus(), OrderStatus.CANCELLED);

        OrderStatus fromStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        OrderTimelineEvent cancelTimeline = new OrderTimelineEvent(
                updated,
                fromStatus,
                OrderStatus.CANCELLED,
                customerId,
                TimelineActorType.CUSTOMER,
                "Cancelled by customer: " + request.getReason()
        );
        timelineRepository.save(cancelTimeline);
        updated.getTimelineEvents().add(cancelTimeline);

        // Transactional Outbox: OrderCancelled event
        try {
            String payloadJson = objectMapper.writeValueAsString(updated.getId());
            OutboxEventRecord outbox = new OutboxEventRecord(
                    "Order",
                    updated.getId().toString(),
                    "OrderCancelled",
                    payloadJson,
                    CorrelationContext.getCorrelationId()
            );
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize OrderCancelled event", e);
        }

        return ResponseEntity.ok(mapToResponse(updated));
    }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setStatus(order.getStatus().name());
        response.setIdempotencyKey(order.getIdempotencyKey());
        response.setShippingRecipientName(order.getShippingRecipientName());
        response.setShippingPhone(order.getShippingPhone());
        response.setShippingLine1(order.getShippingLine1());
        response.setShippingLine2(order.getShippingLine2());
        response.setShippingWard(order.getShippingWard());
        response.setShippingDistrict(order.getShippingDistrict());
        response.setShippingCity(order.getShippingCity());
        response.setSubtotalAmount(order.getSubtotalAmount());
        response.setShippingFeeAmount(order.getShippingFeeAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setGrandTotalAmount(order.getGrandTotalAmount());
        response.setCurrency(order.getCurrency());
        response.setPlacedAt(order.getPlacedAt());

        List<OrderItem> items = order.getItems().isEmpty()
                ? orderItemRepository.findByOrderId(order.getId())
                : order.getItems();

        response.setItems(items.stream().map(i -> new OrderItemResponse(
                i.getId(),
                i.getSkuId(),
                i.getProductNameSnapshot(),
                i.getSkuCodeSnapshot(),
                i.getAttributeSnapshot(),
                i.getUnitPriceSnapshot(),
                i.getQuantity(),
                i.getLineTotal()
        )).collect(Collectors.toList()));

        List<OrderTimelineEvent> events = order.getTimelineEvents().isEmpty()
                ? timelineRepository.findByOrderIdOrderByOccurredAtAsc(order.getId())
                : order.getTimelineEvents();

        response.setTimeline(events.stream().map(t -> new OrderTimelineEventResponse(
                t.getId(),
                t.getOrder().getId(),
                t.getFromStatus() != null ? t.getFromStatus().name() : null,
                t.getToStatus().name(),
                t.getActorId(),
                t.getActorType().name(),
                t.getNote(),
                t.getOccurredAt()
        )).collect(Collectors.toList()));

        return response;
    }
}
