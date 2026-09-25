package com.ecommerce.inventory.service;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.ConflictException;
import com.ecommerce.common.error.InvalidStateException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.inventory.api.dto.*;
import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.model.InventoryReservation;
import com.ecommerce.inventory.domain.model.ReservationStatus;
import com.ecommerce.inventory.domain.repository.InventoryRepository;
import com.ecommerce.inventory.domain.repository.InventoryReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final InventoryRedisCacheService cacheService;
    private final InventoryOutboxService outboxService;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryReservationRepository reservationRepository,
                            InventoryRedisCacheService cacheService,
                            InventoryOutboxService outboxService) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.cacheService = cacheService;
        this.outboxService = outboxService;
    }

    // ==========================================
    // API-INV-001: Get Available Inventory & Breakdown
    // ==========================================
    @Transactional(readOnly = true)
    public InventoryDto getInventory(UUID skuId) {
        if (skuId == null) {
            throw new IllegalArgumentException("SKU ID cannot be null");
        }

        Inventory inventory = inventoryRepository.findBySkuId(skuId)
                .orElseThrow(() -> new NotFoundException("Inventory record not found for SKU: " + skuId));

        cacheService.cacheAvailableStock(skuId, inventory.getQuantityAvailable());

        return new InventoryDto(
                inventory.getSkuId(),
                inventory.getQuantityOnHand(),
                inventory.getQuantityReserved(),
                inventory.getQuantityAvailable()
        );
    }

    // ==========================================
    // API-INV-002: Idempotent Stock Reservation with TTL
    // ==========================================
    public ReserveStockResponse reserveStock(ReserveStockRequest request) {
        if (request == null || request.getOrderId() == null) {
            throw new IllegalArgumentException("Reserve request and Order ID must not be null");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessRuleException("BR-004", "Reservation items list cannot be empty");
        }

        UUID orderId = request.getOrderId();

        // 1. Idempotency Check
        List<InventoryReservation> existing = reservationRepository.findByOrderId(orderId);
        if (!existing.isEmpty()) {
            boolean allActive = existing.stream().allMatch(r -> r.getStatus() == ReservationStatus.ACTIVE);
            if (allActive) {
                log.info("Idempotent reservation replay for order: {}", orderId);
                List<UUID> reservationIds = existing.stream().map(InventoryReservation::getId).toList();
                return new ReserveStockResponse(orderId, reservationIds, "ACTIVE", existing.get(0).getExpiresAt());
            }
            throw new ConflictException("Reservation for order " + orderId + " is already in state: " +
                    existing.get(0).getStatus());
        }

        // 2. Deterministic Row Locking Order to Prevent Deadlocks
        List<ReservationItemRequest> sortedItems = new ArrayList<>(request.getItems());
        sortedItems.sort(Comparator.comparing(ReservationItemRequest::getSkuId));

        // 3. Pessimistic Write Locking & Stock Validation for ALL Items (All-or-Nothing)
        List<Inventory> lockedInventories = new ArrayList<>();
        for (ReservationItemRequest item : sortedItems) {
            Inventory inv = inventoryRepository.findBySkuIdWithPessimisticLock(item.getSkuId())
                    .orElseThrow(() -> new NotFoundException("Inventory record not found for SKU: " + item.getSkuId()));

            if (inv.getQuantityAvailable() < item.getQuantity()) {
                outboxService.recordEvent(
                        "INVENTORY_RESERVATION",
                        orderId.toString(),
                        "InventoryReservationFailed",
                        Map.of(
                                "order_id", orderId.toString(),
                                "sku_id", item.getSkuId().toString(),
                                "requested", item.getQuantity(),
                                "available", inv.getQuantityAvailable()
                        )
                );
                throw new BusinessRuleException("BR-004", "[BR-004] Insufficient available inventory for SKU: " +
                        item.getSkuId() + ". Available: " + inv.getQuantityAvailable() + ", Requested: " + item.getQuantity());
            }
            lockedInventories.add(inv);
        }

        // 4. Apply Reservation
        int ttlMinutes = (request.getTtlMinutes() != null && request.getTtlMinutes() > 0) ? request.getTtlMinutes() : 15;
        Instant expiresAt = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);
        List<UUID> reservationIds = new ArrayList<>();

        for (int i = 0; i < sortedItems.size(); i++) {
            ReservationItemRequest item = sortedItems.get(i);
            Inventory inv = lockedInventories.get(i);

            inv.reserve(item.getQuantity());
            inventoryRepository.save(inv);

            InventoryReservation reservation = new InventoryReservation(
                    item.getSkuId(),
                    orderId,
                    item.getQuantity(),
                    expiresAt
            );
            InventoryReservation savedRes = reservationRepository.save(reservation);
            reservationIds.add(savedRes.getId());

            cacheService.evict(item.getSkuId());
        }

        // 5. Publish Outbox Event
        outboxService.recordEvent(
                "INVENTORY_RESERVATION",
                orderId.toString(),
                "InventoryReserved",
                Map.of(
                        "order_id", orderId.toString(),
                        "reservation_ids", reservationIds,
                        "expires_at", expiresAt.toString()
                )
        );

        return new ReserveStockResponse(orderId, reservationIds, "ACTIVE", expiresAt);
    }

    // ==========================================
    // API-INV-003: Release Reserved Stock
    // ==========================================
    public ReleaseStockResponse releaseStock(UUID orderId, String reason) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        List<InventoryReservation> reservations = reservationRepository.findByOrderId(orderId);
        if (reservations.isEmpty()) {
            throw new NotFoundException("No reservations found for order: " + orderId);
        }

        // Idempotency: if already released or expired, return success immediately
        boolean allReleasedOrExpired = reservations.stream()
                .allMatch(r -> r.getStatus() == ReservationStatus.RELEASED || r.getStatus() == ReservationStatus.EXPIRED);
        if (allReleasedOrExpired) {
            log.info("Idempotent release replay for order: {}", orderId);
            return new ReleaseStockResponse(orderId, reservations.get(0).getStatus().name());
        }

        // State Machine validation: cannot release already consumed reservation
        boolean hasConsumed = reservations.stream().anyMatch(r -> r.getStatus() == ReservationStatus.CONSUMED);
        if (hasConsumed) {
            throw new InvalidStateException("Cannot release reservation for order " + orderId +
                    " because it is already CONSUMED");
        }

        // Release all active reservations
        for (InventoryReservation res : reservations) {
            if (res.getStatus() == ReservationStatus.ACTIVE) {
                Inventory inv = inventoryRepository.findBySkuIdWithPessimisticLock(res.getSkuId())
                        .orElseThrow(() -> new NotFoundException("Inventory record not found for SKU: " + res.getSkuId()));

                inv.release(res.getQuantity());
                inventoryRepository.save(inv);

                res.release();
                reservationRepository.save(res);

                cacheService.evict(res.getSkuId());
            }
        }

        outboxService.recordEvent(
                "INVENTORY_RESERVATION",
                orderId.toString(),
                "InventoryReleased",
                Map.of(
                        "order_id", orderId.toString(),
                        "reason", reason != null ? reason : "PAYMENT_FAILED_OR_CANCELLED"
                )
        );

        return new ReleaseStockResponse(orderId, "RELEASED");
    }

    // ==========================================
    // API-INV-004: Commit Reserved Stock on Payment Success
    // ==========================================
    public CommitStockResponse commitStock(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("Order ID cannot be null");
        }

        List<InventoryReservation> reservations = reservationRepository.findByOrderId(orderId);
        if (reservations.isEmpty()) {
            throw new NotFoundException("No reservations found for order: " + orderId);
        }

        // Idempotency: if already consumed, return success immediately
        boolean allConsumed = reservations.stream().allMatch(r -> r.getStatus() == ReservationStatus.CONSUMED);
        if (allConsumed) {
            log.info("Idempotent commit replay for order: {}", orderId);
            return new CommitStockResponse(orderId, "CONSUMED");
        }

        // State Machine validation: cannot commit released or expired reservations
        boolean hasReleasedOrExpired = reservations.stream()
                .anyMatch(r -> r.getStatus() == ReservationStatus.RELEASED || r.getStatus() == ReservationStatus.EXPIRED);
        if (hasReleasedOrExpired) {
            throw new InvalidStateException("Cannot commit reservation for order " + orderId +
                    " because it is in terminal state: " + reservations.get(0).getStatus());
        }

        // Commit all active reservations
        for (InventoryReservation res : reservations) {
            if (res.getStatus() == ReservationStatus.ACTIVE) {
                Inventory inv = inventoryRepository.findBySkuIdWithPessimisticLock(res.getSkuId())
                        .orElseThrow(() -> new NotFoundException("Inventory record not found for SKU: " + res.getSkuId()));

                inv.commit(res.getQuantity());
                inventoryRepository.save(inv);

                res.consume();
                reservationRepository.save(res);

                cacheService.evict(res.getSkuId());
            }
        }

        outboxService.recordEvent(
                "INVENTORY_RESERVATION",
                orderId.toString(),
                "InventoryCommitted",
                Map.of("order_id", orderId.toString())
        );

        return new CommitStockResponse(orderId, "CONSUMED");
    }

    // ==========================================
    // Background / Scheduled: Expire Stale Reservations (INV-T04)
    // ==========================================
    public int expireStaleReservations() {
        Instant now = Instant.now();
        List<InventoryReservation> expiredList = reservationRepository
                .findByStatusAndExpiresAtBefore(ReservationStatus.ACTIVE, now);

        int count = 0;
        for (InventoryReservation res : expiredList) {
            try {
                Inventory inv = inventoryRepository.findBySkuIdWithPessimisticLock(res.getSkuId()).orElse(null);
                if (inv != null) {
                    inv.release(res.getQuantity());
                    inventoryRepository.save(inv);
                }

                res.expire();
                reservationRepository.save(res);

                cacheService.evict(res.getSkuId());
                outboxService.recordEvent(
                        "INVENTORY_RESERVATION",
                        res.getId().toString(),
                        "InventoryReservationExpired",
                        Map.of(
                                "order_id", res.getOrderId().toString(),
                                "sku_id", res.getSkuId().toString(),
                                "quantity", res.getQuantity()
                        )
                );
                count++;
            } catch (Exception e) {
                log.error("Failed to expire reservation: {}", res.getId(), e);
            }
        }

        return count;
    }
}
