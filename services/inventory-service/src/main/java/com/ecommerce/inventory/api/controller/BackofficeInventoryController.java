package com.ecommerce.inventory.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.inventory.api.dto.AdjustInventoryRequest;
import com.ecommerce.inventory.api.dto.InventoryAdjustmentLogDto;
import com.ecommerce.inventory.api.dto.InventoryDto;
import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.model.InventoryAdjustmentLog;
import com.ecommerce.inventory.domain.repository.InventoryAdjustmentLogRepository;
import com.ecommerce.inventory.domain.repository.InventoryRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/skus/{skuId}/inventory")
public class BackofficeInventoryController {

    private final InventoryRepository inventoryRepository;
    private final InventoryAdjustmentLogRepository adjustmentLogRepository;

    public BackofficeInventoryController(InventoryRepository inventoryRepository,
                                         InventoryAdjustmentLogRepository adjustmentLogRepository) {
        this.inventoryRepository = inventoryRepository;
        this.adjustmentLogRepository = adjustmentLogRepository;
    }

    @PostMapping("/adjustments")
    @Transactional
    public ResponseEntity<InventoryDto> adjustInventory(
            @PathVariable UUID skuId,
            @RequestParam UUID actorId,
            @Valid @RequestBody AdjustInventoryRequest request) {

        Inventory inventory = inventoryRepository.findBySkuIdWithPessimisticLock(skuId)
                .orElseGet(() -> inventoryRepository.save(new Inventory(skuId, 0)));

        int quantityBefore = inventory.getQuantityOnHand();
        int quantityAfter = quantityBefore + request.getDelta();

        if (quantityAfter < 0) {
            throw new BusinessRuleException("BR-004", "Inventory on-hand quantity cannot be negative: " + quantityAfter);
        }

        inventory.setQuantityOnHand(quantityAfter);
        Inventory saved = inventoryRepository.save(inventory);

        // Immutable audit log write per BR-015
        InventoryAdjustmentLog logEntry = new InventoryAdjustmentLog(
                skuId,
                actorId,
                quantityBefore,
                quantityAfter,
                request.getDelta(),
                request.getReasonCode(),
                request.getNote()
        );
        adjustmentLogRepository.save(logEntry);

        return ResponseEntity.ok(new InventoryDto(
                saved.getSkuId(),
                saved.getQuantityOnHand(),
                saved.getQuantityReserved(),
                saved.getQuantityAvailable()
        ));
    }

    @GetMapping("/audit-log")
    public ResponseEntity<Page<InventoryAdjustmentLogDto>> getAuditLog(
            @PathVariable UUID skuId,
            Pageable pageable) {

        Page<InventoryAdjustmentLog> page = adjustmentLogRepository.findBySkuIdOrderByCreatedAtDesc(skuId, pageable);
        Page<InventoryAdjustmentLogDto> dtos = page.map(l -> new InventoryAdjustmentLogDto(
                l.getId(),
                l.getSkuId(),
                l.getActorId(),
                l.getQuantityBefore(),
                l.getQuantityAfter(),
                l.getDelta(),
                l.getReasonCode().name(),
                l.getNote(),
                l.getCreatedAt()
        ));

        return ResponseEntity.ok(dtos);
    }
}
