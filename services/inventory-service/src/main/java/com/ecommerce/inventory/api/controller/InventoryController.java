package com.ecommerce.inventory.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.inventory.api.dto.InventoryDto;
import com.ecommerce.inventory.api.dto.ReservationItemRequest;
import com.ecommerce.inventory.api.dto.ReserveStockRequest;
import com.ecommerce.inventory.api.dto.ReserveStockResponse;
import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.model.InventoryReservation;
import com.ecommerce.inventory.domain.model.ReservationStatus;
import com.ecommerce.inventory.domain.repository.InventoryRepository;
import com.ecommerce.inventory.domain.repository.InventoryReservationRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;

    public InventoryController(InventoryRepository inventoryRepository,
                               InventoryReservationRepository reservationRepository) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    @GetMapping("/skus/{skuId}")
    public ResponseEntity<InventoryDto> getStock(@PathVariable UUID skuId) {
        Inventory inventory = inventoryRepository.findBySkuId(skuId)
                .orElseThrow(() -> new NotFoundException("Inventory record not found for SKU: " + skuId));

        return ResponseEntity.ok(new InventoryDto(
                inventory.getSkuId(),
                inventory.getQuantityOnHand(),
                inventory.getQuantityReserved(),
                inventory.getQuantityAvailable()
        ));
    }

    @PostMapping("/reservations")
    @Transactional
    public ResponseEntity<ReserveStockResponse> reserveStock(@Valid @RequestBody ReserveStockRequest request) {
        // All-or-nothing check and reserve with pessimistic lock
        List<Inventory> lockedInventories = new ArrayList<>();
        for (ReservationItemRequest item : request.getItems()) {
            Inventory inv = inventoryRepository.findBySkuIdWithPessimisticLock(item.getSkuId())
                    .orElseThrow(() -> new NotFoundException("Inventory record not found for SKU: " + item.getSkuId()));

            if (inv.getQuantityAvailable() < item.getQuantity()) {
                throw new BusinessRuleException("BR-004", "Insufficient available inventory for SKU: " + item.getSkuId());
            }
            lockedInventories.add(inv);
        }

        // Apply reservation and save
        List<UUID> reservationIds = new ArrayList<>();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        for (int i = 0; i < request.getItems().size(); i++) {
            ReservationItemRequest item = request.getItems().get(i);
            Inventory inv = lockedInventories.get(i);

            inv.setQuantityReserved(inv.getQuantityReserved() + item.getQuantity());
            inventoryRepository.save(inv);

            InventoryReservation reservation = new InventoryReservation(
                    item.getSkuId(),
                    request.getOrderId(),
                    item.getQuantity(),
                    expiresAt
            );
            InventoryReservation saved = reservationRepository.save(reservation);
            reservationIds.add(saved.getId());
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ReserveStockResponse(request.getOrderId(), reservationIds, "ACTIVE"));
    }

    @PostMapping("/reservations/orders/{orderId}/consume")
    @Transactional
    public ResponseEntity<Void> consumeReservation(@PathVariable UUID orderId) {
        List<InventoryReservation> reservations = reservationRepository.findByOrderId(orderId);
        for (InventoryReservation res : reservations) {
            if (res.getStatus() == ReservationStatus.ACTIVE) {
                res.setStatus(ReservationStatus.CONSUMED);
                reservationRepository.save(res);

                Inventory inv = inventoryRepository.findBySkuIdWithPessimisticLock(res.getSkuId())
                        .orElseThrow(() -> new NotFoundException("Inventory not found for SKU: " + res.getSkuId()));
                inv.setQuantityOnHand(inv.getQuantityOnHand() - res.getQuantity());
                inv.setQuantityReserved(inv.getQuantityReserved() - res.getQuantity());
                inventoryRepository.save(inv);
            }
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reservations/orders/{orderId}/release")
    @Transactional
    public ResponseEntity<Void> releaseReservation(@PathVariable UUID orderId) {
        List<InventoryReservation> reservations = reservationRepository.findByOrderId(orderId);
        for (InventoryReservation res : reservations) {
            if (res.getStatus() == ReservationStatus.ACTIVE) {
                res.setStatus(ReservationStatus.RELEASED);
                reservationRepository.save(res);

                Inventory inv = inventoryRepository.findBySkuIdWithPessimisticLock(res.getSkuId())
                        .orElseThrow(() -> new NotFoundException("Inventory not found for SKU: " + res.getSkuId()));
                inv.setQuantityReserved(inv.getQuantityReserved() - res.getQuantity());
                inventoryRepository.save(inv);
            }
        }
        return ResponseEntity.ok().build();
    }
}
