package com.ecommerce.inventory.service;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.inventory.api.dto.AdjustInventoryRequest;
import com.ecommerce.inventory.api.dto.InventoryAdjustmentLogDto;
import com.ecommerce.inventory.api.dto.InventoryDto;
import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.model.InventoryAdjustmentLog;
import com.ecommerce.inventory.domain.repository.InventoryAdjustmentLogRepository;
import com.ecommerce.inventory.domain.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class InventoryAdjustmentService {

    private static final Logger log = LoggerFactory.getLogger(InventoryAdjustmentService.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryAdjustmentLogRepository adjustmentLogRepository;
    private final InventoryRedisCacheService cacheService;
    private final InventoryOutboxService outboxService;

    public InventoryAdjustmentService(InventoryRepository inventoryRepository,
                                     InventoryAdjustmentLogRepository adjustmentLogRepository,
                                     InventoryRedisCacheService cacheService,
                                     InventoryOutboxService outboxService) {
        this.inventoryRepository = inventoryRepository;
        this.adjustmentLogRepository = adjustmentLogRepository;
        this.cacheService = cacheService;
        this.outboxService = outboxService;
    }

    public InventoryDto adjustInventory(UUID skuId, UUID actorId, AdjustInventoryRequest request) {
        if (skuId == null) {
            throw new IllegalArgumentException("SKU ID cannot be null");
        }
        if (actorId == null) {
            throw new IllegalArgumentException("Actor ID cannot be null");
        }
        if (request == null || request.getDelta() == null || request.getReasonCode() == null) {
            throw new IllegalArgumentException("Adjust request, delta, and reason code must be provided");
        }

        Inventory inventory = inventoryRepository.findBySkuIdWithPessimisticLock(skuId)
                .orElseGet(() -> inventoryRepository.save(new Inventory(skuId, 0)));

        int quantityBefore = inventory.getQuantityOnHand();

        // Enforces BR-004 (quantityAfter >= 0 and >= quantityReserved)
        inventory.adjust(request.getDelta());
        Inventory saved = inventoryRepository.save(inventory);

        // Immutable audit log per BR-015
        InventoryAdjustmentLog logEntry = new InventoryAdjustmentLog(
                skuId,
                actorId,
                quantityBefore,
                saved.getQuantityOnHand(),
                request.getDelta(),
                request.getReasonCode(),
                request.getNote()
        );
        adjustmentLogRepository.save(logEntry);

        cacheService.evict(skuId);

        outboxService.recordEvent(
                "INVENTORY",
                skuId.toString(),
                "InventoryAdjusted",
                Map.of(
                        "sku_id", skuId.toString(),
                        "actor_id", actorId.toString(),
                        "quantity_before", quantityBefore,
                        "quantity_after", saved.getQuantityOnHand(),
                        "delta", request.getDelta(),
                        "reason_code", request.getReasonCode().name()
                )
        );

        log.info("Inventory adjusted for SKU {}: {} -> {} (delta={}) by actor {}",
                skuId, quantityBefore, saved.getQuantityOnHand(), request.getDelta(), actorId);

        return new InventoryDto(
                saved.getSkuId(),
                saved.getQuantityOnHand(),
                saved.getQuantityReserved(),
                saved.getQuantityAvailable()
        );
    }

    @Transactional(readOnly = true)
    public Page<InventoryAdjustmentLogDto> getAuditLog(UUID skuId, Pageable pageable) {
        if (skuId == null) {
            throw new IllegalArgumentException("SKU ID cannot be null");
        }

        Page<InventoryAdjustmentLog> page = adjustmentLogRepository.findBySkuIdOrderByCreatedAtDesc(skuId, pageable);
        return page.map(l -> new InventoryAdjustmentLogDto(
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
    }
}
