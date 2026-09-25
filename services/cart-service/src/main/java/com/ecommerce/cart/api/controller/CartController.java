package com.ecommerce.cart.api.controller;

import com.ecommerce.cart.api.dto.AddToCartRequest;
import com.ecommerce.cart.api.dto.CartDto;
import com.ecommerce.cart.api.dto.UpdateCartItemRequest;
import com.ecommerce.cart.security.UserPrincipal;
import com.ecommerce.cart.service.CartService;
import com.ecommerce.common.error.AuthenticationFailedException;
import com.ecommerce.common.error.AuthorizationFailedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // ==========================================
    // API-CRT-001: Get active customer cart
    // ==========================================
    @GetMapping({"/cart", "/customers/{customerId}/cart"})
    public ResponseEntity<CartDto> getCart(
            @PathVariable(value = "customerId", required = false) UUID pathCustomerId,
            @RequestParam(value = "customerId", required = false) UUID paramCustomerId,
            Authentication authentication) {
        UUID customerId = resolveCustomerId(pathCustomerId, paramCustomerId, authentication);
        return ResponseEntity.ok(cartService.getCart(customerId));
    }

    // ==========================================
    // API-CRT-002: Add SKU to cart
    // ==========================================
    @PostMapping({"/cart/items", "/customers/{customerId}/cart/items"})
    public ResponseEntity<CartDto> addItem(
            @PathVariable(value = "customerId", required = false) UUID pathCustomerId,
            @RequestParam(value = "customerId", required = false) UUID paramCustomerId,
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {
        UUID customerId = resolveCustomerId(pathCustomerId, paramCustomerId, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItem(customerId, request));
    }

    // ==========================================
    // API-CRT-003: Update item quantity
    // ==========================================
    @PutMapping({"/cart/items/{itemId}", "/customers/{customerId}/cart/items/{itemId}"})
    public ResponseEntity<CartDto> updateItem(
            @PathVariable(value = "customerId", required = false) UUID pathCustomerId,
            @PathVariable("itemId") UUID itemId,
            @RequestParam(value = "customerId", required = false) UUID paramCustomerId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {
        UUID customerId = resolveCustomerId(pathCustomerId, paramCustomerId, authentication);
        return ResponseEntity.ok(cartService.updateItem(customerId, itemId, request));
    }

    // ==========================================
    // API-CRT-004: Remove item from cart
    // ==========================================
    @DeleteMapping({"/cart/items/{itemId}", "/customers/{customerId}/cart/items/{itemId}"})
    public ResponseEntity<CartDto> removeItem(
            @PathVariable(value = "customerId", required = false) UUID pathCustomerId,
            @PathVariable("itemId") UUID itemId,
            @RequestParam(value = "customerId", required = false) UUID paramCustomerId,
            Authentication authentication) {
        UUID customerId = resolveCustomerId(pathCustomerId, paramCustomerId, authentication);
        return ResponseEntity.ok(cartService.removeItem(customerId, itemId));
    }

    // ==========================================
    // API-CRT-005: Clear customer cart
    // ==========================================
    @DeleteMapping({"/cart", "/customers/{customerId}/cart"})
    public ResponseEntity<CartDto> clearCart(
            @PathVariable(value = "customerId", required = false) UUID pathCustomerId,
            @RequestParam(value = "customerId", required = false) UUID paramCustomerId,
            Authentication authentication) {
        UUID customerId = resolveCustomerId(pathCustomerId, paramCustomerId, authentication);
        return ResponseEntity.ok(cartService.clearCart(customerId));
    }

    // ==========================================
    // Customer ID Resolution & Access Control
    // ==========================================
    private UUID resolveCustomerId(UUID pathCustomerId, UUID paramCustomerId, Authentication authentication) {
        UUID targetId = pathCustomerId != null ? pathCustomerId : paramCustomerId;

        Authentication auth = authentication != null ? authentication : org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if (targetId != null) {
                boolean isAdmin = principal.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ADMIN"));
                if (!isAdmin && !principal.getId().equals(targetId)) {
                    throw new AuthorizationFailedException("Access denied: cannot access another customer's cart");
                }
                return targetId;
            }
            return principal.getId();
        }

        if (targetId != null) {
            return targetId;
        }

        if (authentication != null && authentication.getPrincipal() instanceof String principalStr) {
            try {
                return UUID.fromString(principalStr);
            } catch (IllegalArgumentException ignored) {
            }
        }

        throw new AuthenticationFailedException("Authentication required to access cart");
    }
}
