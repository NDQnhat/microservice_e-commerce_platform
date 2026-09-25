package com.ecommerce.inventory.service;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.inventory.api.dto.AdjustInventoryRequest;
import com.ecommerce.inventory.api.dto.InventoryAdjustmentLogDto;
import com.ecommerce.inventory.api.dto.InventoryDto;
import com.ecommerce.inventory.domain.model.AdjustmentReasonCode;
import com.ecommerce.inventory.domain.model.Inventory;
import com.ecommerce.inventory.domain.model.InventoryAdjustmentLog;
import com.ecommerce.inventory.domain.repository.InventoryAdjustmentLogRepository;
import com.ecommerce.inventory.domain.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryAdjustmentServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryAdjustmentLogRepository adjustmentLogRepository;

    @Mock
    private InventoryRedisCacheService cacheService;

    @Mock
    private InventoryOutboxService outboxService;

    @InjectMocks
    private InventoryAdjustmentService adjustmentService;

    private UUID skuId;
    private UUID actorId;
    private Inventory inventory;

    @BeforeEach
    void setUp() {
        skuId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        inventory = new Inventory(skuId, 50);
    }

    @Test
    @DisplayName("adjustInventory positive delta increments on-hand and writes immutable audit log")
    void adjustInventory_PositiveDelta() {
        AdjustInventoryRequest request = new AdjustInventoryRequest(25, AdjustmentReasonCode.RESTOCK, "New shipment arrived");

        when(inventoryRepository.findBySkuIdWithPessimisticLock(skuId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryDto result = adjustmentService.adjustInventory(skuId, actorId, request);

        assertThat(result).isNotNull();
        assertThat(result.getQuantityOnHand()).isEqualTo(75);
        assertThat(result.getQuantityAvailable()).isEqualTo(75);

        verify(adjustmentLogRepository).save(any(InventoryAdjustmentLog.class));
        verify(cacheService).evict(skuId);
        verify(outboxService).recordEvent(eq("INVENTORY"), eq(skuId.toString()), eq("InventoryAdjusted"), any());
    }

    @Test
    @DisplayName("adjustInventory negative delta decrements on-hand successfully")
    void adjustInventory_NegativeDelta() {
        AdjustInventoryRequest request = new AdjustInventoryRequest(-10, AdjustmentReasonCode.DAMAGED, "Damaged during handling");

        when(inventoryRepository.findBySkuIdWithPessimisticLock(skuId)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryDto result = adjustmentService.adjustInventory(skuId, actorId, request);

        assertThat(result).isNotNull();
        assertThat(result.getQuantityOnHand()).isEqualTo(40);
        verify(adjustmentLogRepository).save(any(InventoryAdjustmentLog.class));
    }

    @Test
    @DisplayName("adjustInventory throws BusinessRuleException BR-004 when resulting quantity is negative")
    void adjustInventory_BoundaryNegative_ThrowsBR004() {
        AdjustInventoryRequest request = new AdjustInventoryRequest(-100, AdjustmentReasonCode.CORRECTION, "Stock count discrepancy");

        when(inventoryRepository.findBySkuIdWithPessimisticLock(skuId)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> adjustmentService.adjustInventory(skuId, actorId, request))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> assertThat(((BusinessRuleException) ex).getRuleId()).isEqualTo("BR-004"));

        verify(adjustmentLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAuditLog returns paged audit log DTOs")
    void getAuditLog_ReturnsPagedDtos() {
        Pageable pageable = PageRequest.of(0, 10);
        InventoryAdjustmentLog logEntry = new InventoryAdjustmentLog(
                skuId, actorId, 50, 75, 25, AdjustmentReasonCode.RESTOCK, "Shipment"
        );
        Page<InventoryAdjustmentLog> page = new PageImpl<>(List.of(logEntry));

        when(adjustmentLogRepository.findBySkuIdOrderByCreatedAtDesc(skuId, pageable)).thenReturn(page);

        Page<InventoryAdjustmentLogDto> result = adjustmentService.getAuditLog(skuId, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSkuId()).isEqualTo(skuId);
        assertThat(result.getContent().get(0).getDelta()).isEqualTo(25);
    }
}
