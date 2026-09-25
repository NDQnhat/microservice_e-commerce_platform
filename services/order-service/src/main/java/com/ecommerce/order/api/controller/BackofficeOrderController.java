package com.ecommerce.order.api.controller;

import com.ecommerce.order.api.dto.OrderResponse;
import com.ecommerce.order.api.dto.OrderTransitionRequest;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/orders")
public class BackofficeOrderController {

    private final OrderService orderService;

    public BackofficeOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ==========================================
    // API-ORD-004: List Orders (Backoffice)
    // ==========================================
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID customerId,
            Pageable pageable) {

        return ResponseEntity.ok(orderService.listOrders(status, customerId, pageable));
    }

    // ==========================================
    // API-ORD-005: State Machine Transition (Backoffice)
    // Supports PUT /status, POST /transition, and POST /transitions
    // ==========================================
    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestBody Map<String, Object> body) {

        String statusStr = (String) body.getOrDefault("target_status", body.get("status"));
        if (statusStr == null) {
            throw new IllegalArgumentException("Status or target_status is required");
        }
        OrderStatus targetStatus = OrderStatus.valueOf(statusStr.toUpperCase());
        String note = (String) body.get("note");
        UUID actorId = null;
        if (body.get("actor_id") != null) {
            try {
                actorId = UUID.fromString(body.get("actor_id").toString());
            } catch (Exception ignored) {
            }
        }

        OrderTransitionRequest request = new OrderTransitionRequest(targetStatus, actorId, note);
        return ResponseEntity.ok(orderService.transitionOrderStatus(orderId, request));
    }

    @PostMapping({"/{orderId}/transition", "/{orderId}/transitions"})
    public ResponseEntity<OrderResponse> transitionOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderTransitionRequest request) {

        return ResponseEntity.ok(orderService.transitionOrderStatus(orderId, request));
    }

    // ==========================================
    // API-ORD-006: Stuck Orders Detection
    // ==========================================
    @GetMapping("/stuck")
    public ResponseEntity<List<OrderResponse>> getStuckOrders(
            @RequestParam(defaultValue = "60") int thresholdMinutes) {

        return ResponseEntity.ok(orderService.getStuckOrders(thresholdMinutes));
    }

    // ==========================================
    // API-ORD-007: Stuck Order Event Re-emission
    // ==========================================
    @PostMapping("/{orderId}/retry")
    public ResponseEntity<Void> retryStuckOrder(@PathVariable UUID orderId) {
        orderService.retryStuckOrder(orderId);
        return ResponseEntity.accepted().build();
    }
}
