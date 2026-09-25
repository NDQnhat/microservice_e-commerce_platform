package com.ecommerce.order.api.controller;

import com.ecommerce.common.error.AuthenticationFailedException;
import com.ecommerce.common.error.AuthorizationFailedException;
import com.ecommerce.order.api.dto.CancelOrderRequest;
import com.ecommerce.order.api.dto.CreateOrderRequest;
import com.ecommerce.order.api.dto.OrderResponse;
import com.ecommerce.order.security.SecurityUtils;
import com.ecommerce.order.security.UserPrincipal;
import com.ecommerce.order.service.CreateOrderResult;
import com.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    // ==========================================
    // API-ORD-001: Checkout / Create Order
    // ==========================================
    @PostMapping({"/orders", "/customers/{customerId}/orders"})
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @PathVariable(value = "customerId", required = false) UUID pathCustomerId,
            @RequestParam(value = "customerId", required = false) UUID paramCustomerId,
            @Valid @RequestBody(required = false) CreateOrderRequest request,
            Authentication authentication) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required per NFR-IDEMPOTENCY-001");
        }

        UUID customerId = resolveCustomerId(pathCustomerId, paramCustomerId, authentication, request != null ? request.getCustomerId() : null);

        CreateOrderResult result = orderService.createOrder(customerId, idempotencyKey, request != null ? request : new CreateOrderRequest());

        if (result.isReplay()) {
            return ResponseEntity.ok(result.getResponse());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result.getResponse());
    }

    // ==========================================
    // API-ORD-002: Get Order Detail & Timeline
    // ==========================================
    @GetMapping({"/orders/{orderId}", "/customers/{customerId}/orders/{orderId}"})
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable("orderId") UUID orderId,
            @PathVariable(value = "customerId", required = false) UUID pathCustomerId,
            @RequestParam(value = "customerId", required = false) UUID paramCustomerId,
            Authentication authentication) {

        boolean isAdmin = SecurityUtils.isBackofficeAdmin();
        UUID customerId = null;
        try {
            customerId = resolveCustomerId(pathCustomerId, paramCustomerId, authentication, null);
        } catch (Exception ex) {
            if (!isAdmin) {
                throw ex;
            }
        }

        return ResponseEntity.ok(orderService.getOrder(orderId, customerId, isAdmin));
    }

    // ==========================================
    // API-ORD-003: Cancel Order (Customer/Admin)
    // ==========================================
    @PostMapping({"/orders/{orderId}/cancel", "/customers/{customerId}/orders/{orderId}/cancel"})
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable("orderId") UUID orderId,
            @PathVariable(value = "customerId", required = false) UUID pathCustomerId,
            @RequestParam(value = "customerId", required = false) UUID paramCustomerId,
            @RequestBody(required = false) CancelOrderRequest request,
            Authentication authentication) {

        boolean isAdmin = SecurityUtils.isBackofficeAdmin();
        UUID customerId = null;
        try {
            customerId = resolveCustomerId(pathCustomerId, paramCustomerId, authentication, null);
        } catch (Exception ex) {
            if (!isAdmin) {
                throw ex;
            }
        }

        return ResponseEntity.ok(orderService.cancelOrder(orderId, customerId, isAdmin, request));
    }

    // ==========================================
    // Customer ID Resolution & Access Control
    // ==========================================
    private UUID resolveCustomerId(UUID pathCustomerId, UUID paramCustomerId, Authentication authentication, UUID bodyCustomerId) {
        UUID explicitId = pathCustomerId != null ? pathCustomerId : (paramCustomerId != null ? paramCustomerId : bodyCustomerId);

        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if (explicitId != null) {
                boolean isAdmin = principal.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().contains("ADMIN"));
                if (!isAdmin && !principal.getId().equals(explicitId)) {
                    throw new AuthorizationFailedException("Access denied: cannot operate on another customer's order");
                }
                return explicitId;
            }
            return principal.getId();
        }

        if (explicitId != null) {
            return explicitId;
        }

        if (auth != null && auth.getPrincipal() instanceof String principalStr) {
            try {
                return UUID.fromString(principalStr);
            } catch (IllegalArgumentException ignored) {
            }
        }

        throw new AuthenticationFailedException("Authentication required to access order");
    }
}
