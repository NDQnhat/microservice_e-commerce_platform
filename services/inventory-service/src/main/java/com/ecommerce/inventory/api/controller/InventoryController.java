package com.ecommerce.inventory.api.controller;

import com.ecommerce.inventory.api.dto.*;
import com.ecommerce.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // ==========================================
    // API-INV-001: Get Available Inventory & Breakdown
    // ==========================================
    @GetMapping({"/skus/{skuId}", "/{skuId}"})
    public ResponseEntity<InventoryDto> getStock(@PathVariable("skuId") UUID skuId) {
        return ResponseEntity.ok(inventoryService.getInventory(skuId));
    }

    // ==========================================
    // API-INV-002: Idempotent Stock Reservation
    // ==========================================
    @PostMapping({"/reserve", "/reservations"})
    public ResponseEntity<ReserveStockResponse> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        ReserveStockResponse response = inventoryService.reserveStock(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==========================================
    // API-INV-003: Release Reserved Stock
    // ==========================================
    @PostMapping({"/release", "/reservations/orders/{orderId}/release"})
    public ResponseEntity<ReleaseStockResponse> releaseStock(
            @PathVariable(value = "orderId", required = false) UUID pathOrderId,
            @RequestParam(value = "orderId", required = false) UUID paramOrderId,
            @RequestBody(required = false) ReleaseStockRequest body) {

        UUID orderId = resolveOrderId(pathOrderId, paramOrderId, body != null ? body.getOrderId() : null);
        String reason = body != null ? body.getReason() : "PAYMENT_FAILED_OR_CANCELLED";

        return ResponseEntity.ok(inventoryService.releaseStock(orderId, reason));
    }

    // ==========================================
    // API-INV-004: Commit Reserved Stock on Payment Success
    // ==========================================
    @PostMapping({"/commit", "/reservations/orders/{orderId}/consume"})
    public ResponseEntity<CommitStockResponse> commitStock(
            @PathVariable(value = "orderId", required = false) UUID pathOrderId,
            @RequestParam(value = "orderId", required = false) UUID paramOrderId,
            @RequestBody(required = false) CommitStockRequest body) {

        UUID orderId = resolveOrderId(pathOrderId, paramOrderId, body != null ? body.getOrderId() : null);

        return ResponseEntity.ok(inventoryService.commitStock(orderId));
    }

    private UUID resolveOrderId(UUID pathOrderId, UUID paramOrderId, UUID bodyOrderId) {
        if (pathOrderId != null) return pathOrderId;
        if (bodyOrderId != null) return bodyOrderId;
        if (paramOrderId != null) return paramOrderId;
        throw new IllegalArgumentException("Order ID must be provided in URL path, request body, or query parameter");
    }
}
