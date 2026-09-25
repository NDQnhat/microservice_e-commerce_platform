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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Mock
    private InventoryRedisCacheService cacheService;

    @Mock
    private InventoryOutboxService outboxService;

    @InjectMocks
    private InventoryService inventoryService;

    private UUID skuId1;
    private UUID skuId2;
    private UUID orderId;
    private Inventory inv1;
    private Inventory inv2;

    @BeforeEach
    void setUp() {
        skuId1 = UUID.randomUUID();
        skuId2 = UUID.randomUUID();
        orderId = UUID.randomUUID();

        inv1 = new Inventory(skuId1, 100);
        inv2 = new Inventory(skuId2, 50);
    }

    // ==========================================
    // API-INV-001: Get Inventory
    // ==========================================

    @Test
    @DisplayName("getInventory returns InventoryDto and caches available stock in Redis")
    void getInventory_Success() {
        when(inventoryRepository.findBySkuId(skuId1)).thenReturn(Optional.of(inv1));

        InventoryDto dto = inventoryService.getInventory(skuId1);

        assertThat(dto).isNotNull();
        assertThat(dto.getSkuId()).isEqualTo(skuId1);
        assertThat(dto.getQuantityOnHand()).isEqualTo(100);
        assertThat(dto.getQuantityAvailable()).isEqualTo(100);
        assertThat(dto.getWarehouses()).isNotEmpty();

        verify(cacheService).cacheAvailableStock(skuId1, 100);
    }

    @Test
    @DisplayName("getInventory throws NotFoundException when SKU does not exist")
    void getInventory_NotFound() {
        when(inventoryRepository.findBySkuId(skuId1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getInventory(skuId1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Inventory record not found");
    }

    // ==========================================
    // API-INV-002: Reserve Stock
    // ==========================================

    @Test
    @DisplayName("reserveStock successfully reserves multi-SKU items in deterministic lock order")
    void reserveStock_HappyPath() {
        ReserveStockRequest request = new ReserveStockRequest(orderId, List.of(
                new ReservationItemRequest(skuId1, 10),
                new ReservationItemRequest(skuId2, 5)
        ));

        when(reservationRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());
        when(inventoryRepository.findBySkuIdWithPessimisticLock(skuId1)).thenReturn(Optional.of(inv1));
        when(inventoryRepository.findBySkuIdWithPessimisticLock(skuId2)).thenReturn(Optional.of(inv2));
        when(reservationRepository.save(any(InventoryReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReserveStockResponse response = inventoryService.reserveStock(request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getReservationIds()).hasSize(2);
        assertThat(response.getExpiresAt()).isNotNull();

        assertThat(inv1.getQuantityReserved()).isEqualTo(10);
        assertThat(inv2.getQuantityReserved()).isEqualTo(5);

        verify(cacheService).evict(skuId1);
        verify(cacheService).evict(skuId2);
        verify(outboxService).recordEvent(eq("INVENTORY_RESERVATION"), eq(orderId.toString()), eq("InventoryReserved"), any());
    }

    @Test
    @DisplayName("reserveStock is idempotent and returns existing active reservation without double-reserving")
    void reserveStock_IdempotentReplay() {
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
        InventoryReservation existingRes = new InventoryReservation(skuId1, orderId, 10, expiresAt);

        when(reservationRepository.findByOrderId(orderId)).thenReturn(List.of(existingRes));

        ReserveStockRequest request = new ReserveStockRequest(orderId, List.of(
                new ReservationItemRequest(skuId1, 10)
        ));

        ReserveStockResponse response = inventoryService.reserveStock(request);

        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(orderId);
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getReservationIds()).contains(existingRes.getId());

        verify(inventoryRepository, never()).findBySkuIdWithPessimisticLock(any());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("reserveStock throws ConflictException if existing reservation is in terminal state")
    void reserveStock_Conflict_WhenExistingTerminal() {
        InventoryReservation existingRes = new InventoryReservation(skuId1, orderId, 10, Instant.now());
        existingRes.consume();

        when(reservationRepository.findByOrderId(orderId)).thenReturn(List.of(existingRes));

        ReserveStockRequest request = new ReserveStockRequest(orderId, List.of(
                new ReservationItemRequest(skuId1, 10)
        ));

        assertThatThrownBy(() -> inventoryService.reserveStock(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already in state: CONSUMED");
    }

    @Test
    @DisplayName("reserveStock enforces BR-004 all-or-nothing: throws 422 and saves no reservation if any SKU has insufficient stock")
    void reserveStock_BR004_InsufficientStock() {
        ReserveStockRequest request = new ReserveStockRequest(orderId, List.of(
                new ReservationItemRequest(skuId1, 10),
                new ReservationItemRequest(skuId2, 999) // exceeds 50 available
        ));

        when(reservationRepository.findByOrderId(orderId)).thenReturn(Collections.emptyList());
        lenient().when(inventoryRepository.findBySkuIdWithPessimisticLock(skuId1)).thenReturn(Optional.of(inv1));
        lenient().when(inventoryRepository.findBySkuIdWithPessimisticLock(skuId2)).thenReturn(Optional.of(inv2));

        assertThatThrownBy(() -> inventoryService.reserveStock(request))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getRuleId()).isEqualTo("BR-004"));

        // No inventory modified
        assertThat(inv1.getQuantityReserved()).isEqualTo(0);
        assertThat(inv2.getQuantityReserved()).isEqualTo(0);
        verify(reservationRepository, never()).save(any());
        verify(outboxService).recordEvent(eq("INVENTORY_RESERVATION"), eq(orderId.toString()), eq("InventoryReservationFailed"), any());
    }

    // ==========================================
    // API-INV-003: Release Stock
    // ==========================================

    @Test
    @DisplayName("releaseStock restores reserved inventory and sets status RELEASED")
    void releaseStock_HappyPath() {
        inv1.setQuantityReserved(20);
        InventoryReservation res = new InventoryReservation(skuId1, orderId, 20, Instant.now().plus(15, ChronoUnit.MINUTES));

        when(reservationRepository.findByOrderId(orderId)).thenReturn(List.of(res));
        when(inventoryRepository.findBySkuIdWithPessimisticLock(skuId1)).thenReturn(Optional.of(inv1));

        ReleaseStockResponse response = inventoryService.releaseStock(orderId, "ORDER_CANCELLED");

        assertThat(response.getStatus()).isEqualTo("RELEASED");
        assertThat(inv1.getQuantityReserved()).isEqualTo(0);
        assertThat(res.getStatus()).isEqualTo(ReservationStatus.RELEASED);

        verify(cacheService).evict(skuId1);
        verify(outboxService).recordEvent(eq("INVENTORY_RESERVATION"), eq(orderId.toString()), eq("InventoryReleased"), any());
    }

    @Test
    @DisplayName("releaseStock is idempotent if already RELEASED")
    void releaseStock_Idempotent() {
        InventoryReservation res = new InventoryReservation(skuId1, orderId, 20, Instant.now());
        res.release();

        when(reservationRepository.findByOrderId(orderId)).thenReturn(List.of(res));

        ReleaseStockResponse response = inventoryService.releaseStock(orderId, "ORDER_CANCELLED");

        assertThat(response.getStatus()).isEqualTo("RELEASED");
        verify(inventoryRepository, never()).findBySkuIdWithPessimisticLock(any());
    }

    @Test
    @DisplayName("releaseStock throws InvalidStateException if reservation is CONSUMED (Forbidden Transition)")
    void releaseStock_ForbiddenTransition_WhenConsumed() {
        InventoryReservation res = new InventoryReservation(skuId1, orderId, 20, Instant.now());
        res.consume();

        when(reservationRepository.findByOrderId(orderId)).thenReturn(List.of(res));

        assertThatThrownBy(() -> inventoryService.releaseStock(orderId, "CANCEL"))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("already CONSUMED");
    }

    // ==========================================
    // API-INV-004: Commit Stock
    // ==========================================

    @Test
    @DisplayName("commitStock deducts from onHand and reserved, sets status CONSUMED")
    void commitStock_HappyPath() {
        inv1.setQuantityOnHand(100);
        inv1.setQuantityReserved(20);
        InventoryReservation res = new InventoryReservation(skuId1, orderId, 20, Instant.now().plus(15, ChronoUnit.MINUTES));

        when(reservationRepository.findByOrderId(orderId)).thenReturn(List.of(res));
        when(inventoryRepository.findBySkuIdWithPessimisticLock(skuId1)).thenReturn(Optional.of(inv1));

        CommitStockResponse response = inventoryService.commitStock(orderId);

        assertThat(response.getStatus()).isEqualTo("CONSUMED");
        assertThat(inv1.getQuantityOnHand()).isEqualTo(80);
        assertThat(inv1.getQuantityReserved()).isEqualTo(0);
        assertThat(res.getStatus()).isEqualTo(ReservationStatus.CONSUMED);

        verify(cacheService).evict(skuId1);
        verify(outboxService).recordEvent(eq("INVENTORY_RESERVATION"), eq(orderId.toString()), eq("InventoryCommitted"), any());
    }

    @Test
    @DisplayName("commitStock is idempotent if already CONSUMED")
    void commitStock_Idempotent() {
        InventoryReservation res = new InventoryReservation(skuId1, orderId, 20, Instant.now());
        res.consume();

        when(reservationRepository.findByOrderId(orderId)).thenReturn(List.of(res));

        CommitStockResponse response = inventoryService.commitStock(orderId);

        assertThat(response.getStatus()).isEqualTo("CONSUMED");
        verify(inventoryRepository, never()).findBySkuIdWithPessimisticLock(any());
    }

    @Test
    @DisplayName("commitStock throws InvalidStateException if reservation is RELEASED or EXPIRED")
    void commitStock_ForbiddenTransition_WhenReleased() {
        InventoryReservation res = new InventoryReservation(skuId1, orderId, 20, Instant.now());
        res.release();

        when(reservationRepository.findByOrderId(orderId)).thenReturn(List.of(res));

        assertThatThrownBy(() -> inventoryService.commitStock(orderId))
                .isInstanceOf(InvalidStateException.class)
                .hasMessageContaining("terminal state");
    }

    // ==========================================
    // Stale Reservation Expiry Job (INV-T04)
    // ==========================================

    @Test
    @DisplayName("expireStaleReservations releases stock and sets status EXPIRED for overdue reservations")
    void expireStaleReservations_Success() {
        inv1.setQuantityReserved(15);
        InventoryReservation staleRes = new InventoryReservation(skuId1, orderId, 15, Instant.now().minus(1, ChronoUnit.MINUTES));

        when(reservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.ACTIVE), any(Instant.class)))
                .thenReturn(List.of(staleRes));
        when(inventoryRepository.findBySkuIdWithPessimisticLock(skuId1)).thenReturn(Optional.of(inv1));

        int expiredCount = inventoryService.expireStaleReservations();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(inv1.getQuantityReserved()).isEqualTo(0);
        assertThat(staleRes.getStatus()).isEqualTo(ReservationStatus.EXPIRED);

        verify(cacheService).evict(skuId1);
        verify(outboxService).recordEvent(eq("INVENTORY_RESERVATION"), eq(staleRes.getId().toString()), eq("InventoryReservationExpired"), any());
    }
}
