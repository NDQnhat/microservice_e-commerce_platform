package com.ecommerce.order.api.controller;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.order.api.dto.OrderResponse;
import com.ecommerce.order.api.dto.OrderTimelineEventResponse;
import com.ecommerce.order.api.dto.OrderTransitionRequest;
import com.ecommerce.order.domain.model.*;
import com.ecommerce.order.domain.repository.OrderItemRepository;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.order.domain.repository.OrderTimelineEventRepository;
import com.ecommerce.order.domain.repository.OutboxEventRepository;
import com.ecommerce.order.domain.statemachine.OrderStateMachine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/backoffice/orders")
public class BackofficeOrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTimelineEventRepository timelineRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OrderStateMachine stateMachine;
    private final ObjectMapper objectMapper;

    public BackofficeOrderController(OrderRepository orderRepository,
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

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable) {

        Page<Order> orders = status != null
                ? orderRepository.findByStatusOrderByPlacedAtDesc(status, pageable)
                : orderRepository.findAll(pageable);

        return ResponseEntity.ok(orders.map(this::mapToResponseSummary));
    }

    @PostMapping("/{orderId}/transition")
    @Transactional
    public ResponseEntity<OrderResponse> transitionOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderTransitionRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        // Enforce BR-011: Every state transition must be validated by state machine
        stateMachine.validateTransition(order.getStatus(), request.getTargetStatus());

        OrderStatus fromStatus = order.getStatus();
        order.setStatus(request.getTargetStatus());
        Order saved = orderRepository.save(order);

        OrderTimelineEvent timelineEvent = new OrderTimelineEvent(
                saved,
                fromStatus,
                request.getTargetStatus(),
                request.getActorId(),
                TimelineActorType.BACK_OFFICE,
                request.getNote() != null ? request.getNote() : "Status updated by admin"
        );
        timelineRepository.save(timelineEvent);

        // Transactional Outbox: OrderStatusChanged event
        try {
            String payloadJson = objectMapper.writeValueAsString(saved.getId());
            OutboxEventRecord outbox = new OutboxEventRecord(
                    "Order",
                    saved.getId().toString(),
                    "OrderStatusChanged",
                    payloadJson,
                    CorrelationContext.getCorrelationId()
            );
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize OrderStatusChanged event", e);
        }

        return ResponseEntity.ok(mapToResponseSummary(saved));
    }

    @GetMapping("/stuck")
    public ResponseEntity<List<OrderResponse>> getStuckOrders(
            @RequestParam(defaultValue = "60") int thresholdMinutes) {
        Instant cutoff = Instant.now().minus(thresholdMinutes, ChronoUnit.MINUTES);
        List<Order> stuck = orderRepository.findByStatusAndPlacedAtBefore(OrderStatus.RESERVED, cutoff);

        return ResponseEntity.ok(stuck.stream().map(this::mapToResponseSummary).collect(Collectors.toList()));
    }

    @PostMapping("/{orderId}/retry")
    @Transactional
    public ResponseEntity<Void> retryStuckOrder(@PathVariable UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        // Enforce FR-042 & BR-011: re-publishes event, does NOT force status directly
        try {
            String payloadJson = objectMapper.writeValueAsString(order.getId());
            OutboxEventRecord outbox = new OutboxEventRecord(
                    "Order",
                    order.getId().toString(),
                    "OrderStatusChanged",
                    payloadJson,
                    CorrelationContext.getCorrelationId()
            );
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to re-publish stuck order event", e);
        }

        return ResponseEntity.accepted().build();
    }

    private OrderResponse mapToResponseSummary(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setStatus(order.getStatus().name());
        response.setIdempotencyKey(order.getIdempotencyKey());
        response.setSubtotalAmount(order.getSubtotalAmount());
        response.setShippingFeeAmount(order.getShippingFeeAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setGrandTotalAmount(order.getGrandTotalAmount());
        response.setCurrency(order.getCurrency());
        response.setPlacedAt(order.getPlacedAt());
        return response;
    }
}
