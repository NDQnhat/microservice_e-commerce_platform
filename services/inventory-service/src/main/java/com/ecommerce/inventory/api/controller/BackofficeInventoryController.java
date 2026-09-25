package com.ecommerce.inventory.api.controller;

import com.ecommerce.inventory.api.dto.AdjustInventoryRequest;
import com.ecommerce.inventory.api.dto.InventoryAdjustmentLogDto;
import com.ecommerce.inventory.api.dto.InventoryDto;
import com.ecommerce.inventory.security.SecurityUtils;
import com.ecommerce.inventory.service.InventoryAdjustmentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/skus/{skuId}/inventory")
public class BackofficeInventoryController {

    private final InventoryAdjustmentService adjustmentService;

    public BackofficeInventoryController(InventoryAdjustmentService adjustmentService) {
        this.adjustmentService = adjustmentService;
    }

    @PostMapping("/adjustments")
    public ResponseEntity<InventoryDto> adjustInventory(
            @PathVariable UUID skuId,
            @RequestParam(value = "actorId", required = false) UUID paramActorId,
            @Valid @RequestBody AdjustInventoryRequest request) {

        UUID actorId = paramActorId != null ? paramActorId : SecurityUtils.getCurrentUserIdOrNull();
        if (actorId == null) {
            actorId = UUID.nameUUIDFromBytes("inventory-admin".getBytes());
        }

        InventoryDto dto = adjustmentService.adjustInventory(skuId, actorId, request);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/audit-log")
    public ResponseEntity<Page<InventoryAdjustmentLogDto>> getAuditLog(
            @PathVariable UUID skuId,
            Pageable pageable) {

        Page<InventoryAdjustmentLogDto> page = adjustmentService.getAuditLog(skuId, pageable);
        return ResponseEntity.ok(page);
    }
}
