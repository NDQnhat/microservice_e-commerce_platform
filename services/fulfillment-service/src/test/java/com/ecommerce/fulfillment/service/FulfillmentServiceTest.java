package com.ecommerce.fulfillment.service;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.fulfillment.api.dto.ShipmentDto;
import com.ecommerce.fulfillment.api.dto.UpdateShipmentRequest;
import com.ecommerce.fulfillment.client.OrderClient;
import com.ecommerce.fulfillment.domain.model.Shipment;
import com.ecommerce.fulfillment.domain.model.ShipmentStatus;
import com.ecommerce.fulfillment.domain.repository.ShipmentRepository;
import com.ecommerce.fulfillment.domain.statemachine.ShipmentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FulfillmentServiceTest {

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private OrderClient orderClient;

    @Mock
    private FulfillmentOutboxService outboxService;

    private ShipmentStateMachine stateMachine;
    private FulfillmentServiceImpl fulfillmentService;

    @BeforeEach
    void setUp() {
        stateMachine = new ShipmentStateMachine();
        fulfillmentService = new FulfillmentServiceImpl(shipmentRepository, stateMachine, orderClient, outboxService);
    }

    // ==========================================
    // SHP-T01: initiateShipment Tests
    // ==========================================

    @Test
    @DisplayName("initiateShipment succeeds when order is PAID and triggers ORD-T07")
    void initiateShipment_succeedsWhenOrderPaid() {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderClient.getOrderStatus(orderId)).thenReturn("PAID");
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShipmentDto result = fulfillmentService.initiateShipment(orderId);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getStatus()).isEqualTo("PACKING");
        assertThat(result.getPackedAt()).isNotNull();

        verify(orderClient).updateOrderStatus(eq(orderId), eq("PACKING"), anyString());
        verify(shipmentRepository).save(any(Shipment.class));
    }

    @Test
    @DisplayName("initiateShipment is idempotent when shipment already exists")
    void initiateShipment_idempotentWhenExists() {
        UUID orderId = UUID.randomUUID();
        Shipment existing = new Shipment(orderId);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(existing));

        ShipmentDto result = fulfillmentService.initiateShipment(orderId);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
        assertThat(result.getStatus()).isEqualTo("PACKING");

        verify(shipmentRepository, never()).save(any());
        verify(orderClient, never()).updateOrderStatus(any(), any(), any());
    }

    @Test
    @DisplayName("initiateShipment fails if order has not reached PAID state (BR-011)")
    void initiateShipment_failsWhenOrderNotPaid() {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        when(orderClient.getOrderStatus(orderId)).thenReturn("RESERVED");

        assertThatThrownBy(() -> fulfillmentService.initiateShipment(orderId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011")
                .hasMessageContaining("PAID");

        verify(shipmentRepository, never()).save(any());
    }

    // ==========================================
    // SHP-T02: updateShipment to SHIPPED Tests
    // ==========================================

    @Test
    @DisplayName("updateShipment to SHIPPED succeeds with carrier and tracking, records outbox, and drives ORD-T08")
    void updateShipment_toShipped_succeeds() {
        UUID orderId = UUID.randomUUID();
        Shipment shipment = new Shipment(orderId);
        shipment.setStatus(ShipmentStatus.PACKING);

        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));
        when(orderClient.getOrderStatus(orderId)).thenReturn("PACKING");
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateShipmentRequest request = new UpdateShipmentRequest("VNPost", "VNP123456789", ShipmentStatus.SHIPPED);
        ShipmentDto result = fulfillmentService.updateShipment(orderId, request);

        assertThat(result.getStatus()).isEqualTo("SHIPPED");
        assertThat(result.getCarrierName()).isEqualTo("VNPost");
        assertThat(result.getTrackingCode()).isEqualTo("VNP123456789");
        assertThat(result.getShippedAt()).isNotNull();

        verify(outboxService).recordEvent(anyString(), eq("OrderShipped"), any());
        verify(orderClient).updateOrderStatus(eq(orderId), eq("SHIPPED"), anyString());
    }

    @Test
    @DisplayName("updateShipment to SHIPPED fails if carrier_name is missing (BR-011)")
    void updateShipment_toShipped_failsWithoutCarrier() {
        UUID orderId = UUID.randomUUID();
        Shipment shipment = new Shipment(orderId);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        UpdateShipmentRequest request = new UpdateShipmentRequest(null, "VNP123", ShipmentStatus.SHIPPED);

        assertThatThrownBy(() -> fulfillmentService.updateShipment(orderId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011")
                .hasMessageContaining("Tracking code and carrier name must be provided");

        verify(outboxService, never()).recordEvent(any(), any(), any());
        verify(orderClient, never()).updateOrderStatus(any(), any(), any());
    }

    @Test
    @DisplayName("updateShipment to SHIPPED fails if order has not reached PACKING (BR-011)")
    void updateShipment_toShipped_failsIfOrderNotPacking() {
        UUID orderId = UUID.randomUUID();
        Shipment shipment = new Shipment(orderId);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));
        when(orderClient.getOrderStatus(orderId)).thenReturn("CANCELLED");

        UpdateShipmentRequest request = new UpdateShipmentRequest("VNPost", "VNP123", ShipmentStatus.SHIPPED);

        assertThatThrownBy(() -> fulfillmentService.updateShipment(orderId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011")
                .hasMessageContaining("Cannot ship order");

        verify(outboxService, never()).recordEvent(any(), any(), any());
    }

    // ==========================================
    // SHP-T03: updateShipment to DELIVERED Tests
    // ==========================================

    @Test
    @DisplayName("updateShipment to DELIVERED succeeds from SHIPPED, records outbox, and drives ORD-T09 (COMPLETED)")
    void updateShipment_toDelivered_succeeds() {
        UUID orderId = UUID.randomUUID();
        Shipment shipment = new Shipment(orderId);
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setCarrierName("VNPost");
        shipment.setTrackingCode("VNP123");
        shipment.setShippedAt(Instant.now());

        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));
        when(orderClient.getOrderStatus(orderId)).thenReturn("SHIPPED");
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateShipmentRequest request = new UpdateShipmentRequest(null, null, ShipmentStatus.DELIVERED);
        ShipmentDto result = fulfillmentService.updateShipment(orderId, request);

        assertThat(result.getStatus()).isEqualTo("DELIVERED");
        assertThat(result.getDeliveredAt()).isNotNull();

        verify(outboxService).recordEvent(anyString(), eq("OrderDelivered"), any());
        verify(orderClient).updateOrderStatus(eq(orderId), eq("COMPLETED"), anyString());
    }

    @Test
    @DisplayName("updateShipment cannot skip directly from PACKING to DELIVERED (BR-011)")
    void updateShipment_cannotSkipPackingToDelivered() {
        UUID orderId = UUID.randomUUID();
        Shipment shipment = new Shipment(orderId);
        shipment.setStatus(ShipmentStatus.PACKING);

        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        UpdateShipmentRequest request = new UpdateShipmentRequest(null, null, ShipmentStatus.DELIVERED);

        assertThatThrownBy(() -> fulfillmentService.updateShipment(orderId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011")
                .hasMessageContaining("Direct transition from PACKING to DELIVERED is forbidden");

        verify(outboxService, never()).recordEvent(any(), any(), any());
    }

    @Test
    @DisplayName("updateShipment cannot transition out of DELIVERED terminal state (BR-011)")
    void updateShipment_cannotTransitionOutOfDelivered() {
        UUID orderId = UUID.randomUUID();
        Shipment shipment = new Shipment(orderId);
        shipment.setStatus(ShipmentStatus.DELIVERED);

        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        UpdateShipmentRequest request = new UpdateShipmentRequest("VNPost", "VNP123", ShipmentStatus.SHIPPED);

        assertThatThrownBy(() -> fulfillmentService.updateShipment(orderId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-011")
                .hasMessageContaining("Cannot transition out of DELIVERED terminal state");
    }

    // ==========================================
    // Query & Stuck Shipments (FR-021, FR-042)
    // ==========================================

    @Test
    @DisplayName("getShipmentByOrderId returns shipment when found")
    void getShipmentByOrderId_returnsDto() {
        UUID orderId = UUID.randomUUID();
        Shipment shipment = new Shipment(orderId);
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.of(shipment));

        ShipmentDto result = fulfillmentService.getShipmentByOrderId(orderId);

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("getShipmentByOrderId throws NotFoundException when missing")
    void getShipmentByOrderId_throwsNotFound() {
        UUID orderId = UUID.randomUUID();
        when(shipmentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fulfillmentService.getShipmentByOrderId(orderId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("listShipments filters by status and pageable (API-FUL-002)")
    void listShipments_withStatusFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        Shipment s1 = new Shipment(UUID.randomUUID());
        s1.setStatus(ShipmentStatus.PACKING);
        Page<Shipment> page = new PageImpl<>(List.of(s1), pageable, 1);

        when(shipmentRepository.findByStatus(ShipmentStatus.PACKING, pageable)).thenReturn(page);

        Page<ShipmentDto> result = fulfillmentService.listShipments(ShipmentStatus.PACKING, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("PACKING");
    }

    @Test
    @DisplayName("getStuckShipments returns shipments in PACKING older than threshold (FR-042)")
    void getStuckShipments_returnsStuckList() {
        Shipment stuck = new Shipment(UUID.randomUUID());
        stuck.setStatus(ShipmentStatus.PACKING);
        stuck.setPackedAt(Instant.now().minus(120, ChronoUnit.MINUTES));

        when(shipmentRepository.findByStatusAndPackedAtBefore(eq(ShipmentStatus.PACKING), any(Instant.class)))
                .thenReturn(List.of(stuck));

        List<ShipmentDto> result = fulfillmentService.getStuckShipments(60);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo(stuck.getOrderId());
        verify(shipmentRepository).findByStatusAndPackedAtBefore(eq(ShipmentStatus.PACKING), any(Instant.class));
    }
}
