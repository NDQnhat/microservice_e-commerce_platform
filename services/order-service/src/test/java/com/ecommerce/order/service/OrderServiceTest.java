package com.ecommerce.order.service;

import com.ecommerce.common.error.AuthorizationFailedException;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.order.api.dto.*;
import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.client.PricingClient;
import com.ecommerce.order.client.dto.CartDto;
import com.ecommerce.order.client.dto.CartItemDto;
import com.ecommerce.order.client.dto.PriceResponseDto;
import com.ecommerce.order.client.dto.ReserveStockResponseDto;
import com.ecommerce.order.domain.model.*;
import com.ecommerce.order.domain.repository.OrderItemRepository;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.order.domain.repository.OrderTimelineEventRepository;
import com.ecommerce.order.domain.statemachine.OrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderTimelineEventRepository timelineRepository;

    @Spy
    private OrderStateMachine stateMachine = new OrderStateMachine();

    @Mock
    private OrderOutboxService outboxService;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private PricingClient pricingClient;

    @Mock
    private CartClient cartClient;

    @InjectMocks
    private OrderService orderService;

    private UUID customerId;
    private UUID otherCustomerId;
    private UUID orderId;
    private UUID skuId1;
    private UUID skuId2;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        otherCustomerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        skuId1 = UUID.randomUUID();
        skuId2 = UUID.randomUUID();
        idempotencyKey = "idemp-" + UUID.randomUUID();
    }

    // ==========================================
    // API-ORD-001: Checkout / Create Order
    // ==========================================

    @Test
    @DisplayName("API-ORD-001 Happy Path: Create order with explicit items, snapshot prices & reserve stock")
    void createOrder_HappyPath_WithExplicitItems() {
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(skuId1, "Product 1", "SKU-001", "Color: Blue", new BigDecimal("150000.00"), 2),
                new OrderItemRequest(skuId2, "Product 2", "SKU-002", "Color: Red", new BigDecimal("200000.00"), 1)
        );

        CreateOrderRequest request = new CreateOrderRequest(
                customerId, "Nguyen Van A", "0901234567",
                "123 Nguyen Hue", null, "Ben Nghe",
                "District 1", "Ho Chi Minh City", new BigDecimal("30000.00"),
                "VND", items
        );

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(inventoryClient.reserveStock(any(UUID.class), anyList(), anyInt()))
                .thenReturn(new ReserveStockResponseDto(orderId, List.of(UUID.randomUUID()), "ACTIVE"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderResult result = orderService.createOrder(customerId, idempotencyKey, request);

        assertThat(result.isReplay()).isFalse();
        OrderResponse response = result.getResponse();
        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo(customerId);
        assertThat(response.getStatus()).isEqualTo("RESERVED");
        assertThat(response.getIdempotencyKey()).isEqualTo(idempotencyKey);
        // Subtotal: 150000 * 2 + 200000 * 1 = 500000.00
        assertThat(response.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(response.getShippingFeeAmount()).isEqualByComparingTo(new BigDecimal("30000.00"));
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        // Grand total: 500000 + 30000 = 530000.00
        assertThat(response.getGrandTotalAmount()).isEqualByComparingTo(new BigDecimal("530000.00"));

        verify(inventoryClient).reserveStock(any(UUID.class), anyList(), eq(15));
        verify(timelineRepository).save(any(OrderTimelineEvent.class));
        verify(outboxService).recordEvent(anyString(), eq("OrderCreated"), any());
    }

    @Test
    @DisplayName("API-ORD-001: Create order from active customer cart when request items empty")
    void createOrder_HappyPath_FromCart() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(customerId);
        request.setShippingFeeAmount(new BigDecimal("25000.00"));

        CartDto cartDto = new CartDto(UUID.randomUUID(), customerId, "ACTIVE", List.of(
                new CartItemDto(UUID.randomUUID(), skuId1, 3)
        ));
        when(cartClient.getCart(customerId)).thenReturn(cartDto);
        when(pricingClient.getEffectivePrice(skuId1))
                .thenReturn(new PriceResponseDto(skuId1, new BigDecimal("120000.00"), null, new BigDecimal("120000.00"), "VND"));
        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(inventoryClient.reserveStock(any(UUID.class), anyList(), anyInt()))
                .thenReturn(new ReserveStockResponseDto(orderId, List.of(UUID.randomUUID()), "ACTIVE"));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderResult result = orderService.createOrder(customerId, idempotencyKey, request);

        assertThat(result.isReplay()).isFalse();
        OrderResponse response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo("RESERVED");
        // 120000 * 3 = 360000.00
        assertThat(response.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("360000.00"));
        assertThat(response.getGrandTotalAmount()).isEqualByComparingTo(new BigDecimal("385000.00"));
        verify(cartClient).clearCart(customerId);
    }

    @Test
    @DisplayName("BR-010: Idempotent replay returns existing order with isReplay=true without new reservation")
    void createOrder_IdempotencyReplay_ReturnsExistingOrder() {
        Order existing = new Order(
                customerId, idempotencyKey, "Nguyen Van A", "0901234567",
                "123 Main St", null, "Ward 1", "District 1", "HCMC",
                new BigDecimal("500000.00"), new BigDecimal("30000.00"),
                BigDecimal.ZERO, new BigDecimal("530000.00"), "VND"
        );
        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

        CreateOrderRequest request = new CreateOrderRequest();
        CreateOrderResult result = orderService.createOrder(customerId, idempotencyKey, request);

        assertThat(result.isReplay()).isTrue();
        assertThat(result.getResponse().getId()).isEqualTo(existing.getId());
        verify(inventoryClient, never()).reserveStock(any(), any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("BR-010: Idempotency key reused by different customer throws BusinessRuleException")
    void createOrder_IdempotencyConflict_DifferentCustomer_ThrowsException() {
        Order existing = new Order();
        existing.setCustomerId(otherCustomerId);
        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

        CreateOrderRequest request = new CreateOrderRequest();
        assertThatThrownBy(() -> orderService.createOrder(customerId, idempotencyKey, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-010");
    }

    @Test
    @DisplayName("NFR-IDEMPOTENCY-001: Missing or blank idempotency key throws IllegalArgumentException")
    void createOrder_MissingIdempotencyKey_ThrowsException() {
        CreateOrderRequest request = new CreateOrderRequest();
        assertThatThrownBy(() -> orderService.createOrder(customerId, null, request))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orderService.createOrder(customerId, "   ", request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("BR-009: Unauthenticated checkout throws BusinessRuleException BR-009")
    void createOrder_UnauthenticatedGuest_ThrowsBR009() {
        CreateOrderRequest request = new CreateOrderRequest();
        assertThatThrownBy(() -> orderService.createOrder(null, idempotencyKey, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-009");
    }

    @Test
    @DisplayName("BR-004: Empty items and empty cart throws BusinessRuleException BR-004")
    void createOrder_EmptyCartAndItems_ThrowsBR004() {
        CreateOrderRequest request = new CreateOrderRequest();
        when(cartClient.getCart(customerId)).thenReturn(new CartDto(UUID.randomUUID(), customerId, "ACTIVE", Collections.emptyList()));

        assertThatThrownBy(() -> orderService.createOrder(customerId, idempotencyKey, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-004");
    }

    @Test
    @DisplayName("FR-019 / BR-004: Insufficient inventory reservation throws BR-004 and saves no order")
    void createOrder_InsufficientInventory_ThrowsBR004() {
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(skuId1, "Product 1", "SKU-001", "Default", new BigDecimal("100000.00"), 10)
        );
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(items);

        when(orderRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(inventoryClient.reserveStock(any(UUID.class), anyList(), anyInt()))
                .thenThrow(new BusinessRuleException("BR-004", "Insufficient inventory for SKU: " + skuId1));

        assertThatThrownBy(() -> orderService.createOrder(customerId, idempotencyKey, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-004");

        verify(orderRepository, never()).save(any());
        verify(outboxService, never()).recordEvent(any(), any(), any());
    }

    // ==========================================
    // API-ORD-002: Get Order Detail & Timeline
    // ==========================================

    @Test
    @DisplayName("API-ORD-002: Customer can get their own order with timeline")
    void getOrder_CustomerOwnsOrder_Success() {
        Order order = new Order(
                customerId, idempotencyKey, "Nguyen Van A", "0901234567",
                "123 Main St", null, "Ward 1", "District 1", "HCMC",
                new BigDecimal("100000.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, new BigDecimal("100000.00"), "VND"
        );
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(orderId, customerId, false);

        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("API-ORD-002: Accessing another customer's order throws AuthorizationFailedException")
    void getOrder_CustomerDoesNotOwnOrder_ThrowsAuthorizationFailedException() {
        Order order = new Order();
        order.setCustomerId(otherCustomerId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrder(orderId, customerId, false))
                .isInstanceOf(AuthorizationFailedException.class);
    }

    @Test
    @DisplayName("API-ORD-002: Admin can access any order")
    void getOrder_AdminAccessesAnyOrder_Success() {
        Order order = new Order();
        order.setCustomerId(otherCustomerId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrder(orderId, customerId, true);

        assertThat(response).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo(otherCustomerId);
    }

    @Test
    @DisplayName("API-ORD-002: Non-existent order throws NotFoundException")
    void getOrder_NotFound_ThrowsNotFoundException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(orderId, customerId, false))
                .isInstanceOf(NotFoundException.class);
    }

    // ==========================================
    // API-ORD-003: Cancel Order (Customer/Admin)
    // ==========================================

    @Test
    @DisplayName("API-ORD-003: Customer cancels order before cutoff (RESERVED state) -> CANCELLED & inventory released")
    void cancelOrder_CustomerBeforeCutoff_Success() {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.RESERVED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancelOrder(orderId, customerId, false, new CancelOrderRequest("Changed mind"));

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        verify(inventoryClient).releaseStock(eq(orderId), anyString());
        verify(timelineRepository).save(any(OrderTimelineEvent.class));
        verify(outboxService).recordEvent(anyString(), eq("OrderCancelled"), any());
    }

    @Test
    @DisplayName("API-ORD-003: Customer cannot cancel another customer's order")
    void cancelOrder_CustomerDoesNotOwnOrder_ThrowsAuthorizationFailedException() {
        Order order = new Order();
        order.setCustomerId(otherCustomerId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, customerId, false, new CancelOrderRequest("Reason")))
                .isInstanceOf(AuthorizationFailedException.class);
    }

    @Test
    @DisplayName("BR-006: Cancellation attempt after PACKING throws BusinessRuleException BR-006")
    void cancelOrder_AfterCutoff_Packing_ThrowsBR006() {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PACKING);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, customerId, false, new CancelOrderRequest("Too late")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-006");

        verify(orderRepository, never()).save(any());
        verify(inventoryClient, never()).releaseStock(any(), any());
    }

    @Test
    @DisplayName("BR-001: Cancelling already CANCELLED order throws BusinessRuleException BR-001")
    void cancelOrder_AlreadyCancelled_ThrowsBR001() {
        Order order = new Order();
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, customerId, false, new CancelOrderRequest("Already cancelled")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-001");
    }

    // ==========================================
    // API-ORD-004 & API-ORD-005: Backoffice Order Management
    // ==========================================

    @Test
    @DisplayName("API-ORD-004: Backoffice lists orders with status and customer filtering")
    void listOrders_Filtered() {
        Order order = new Order();
        Page<Order> page = new PageImpl<>(List.of(order));
        when(orderRepository.findByCustomerIdAndStatusOrderByPlacedAtDesc(eq(customerId), eq(OrderStatus.RESERVED), any()))
                .thenReturn(page);

        Page<OrderResponse> result = orderService.listOrders(OrderStatus.RESERVED, customerId, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("API-ORD-005: Admin advances order from PAID to PACKING through state machine")
    void transitionOrderStatus_ValidTransition_Success() {
        Order order = new Order();
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderTransitionRequest request = new OrderTransitionRequest(OrderStatus.PACKING, UUID.randomUUID(), "Packing started");
        OrderResponse response = orderService.transitionOrderStatus(orderId, request);

        assertThat(response.getStatus()).isEqualTo("PACKING");
        verify(timelineRepository).save(any(OrderTimelineEvent.class));
        verify(outboxService).recordEvent(anyString(), eq("OrderStatusChanged"), any());
    }

    @Test
    @DisplayName("API-ORD-005: Admin transition to CANCELLED releases inventory")
    void transitionOrderStatus_ToCancelled_ReleasesInventory() {
        Order order = new Order();
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderTransitionRequest request = new OrderTransitionRequest(OrderStatus.CANCELLED, UUID.randomUUID(), "Admin cancel");
        OrderResponse response = orderService.transitionOrderStatus(orderId, request);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        verify(inventoryClient).releaseStock(eq(orderId), anyString());
        verify(outboxService).recordEvent(anyString(), eq("OrderCancelled"), any());
    }

    @Test
    @DisplayName("BR-007 / BR-011: Admin attempting invalid state transition throws BR-007")
    void transitionOrderStatus_InvalidTransition_ThrowsBR007() {
        Order order = new Order();
        order.setStatus(OrderStatus.RESERVED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        OrderTransitionRequest request = new OrderTransitionRequest(OrderStatus.SHIPPED); // Skipping intermediate states!

        assertThatThrownBy(() -> orderService.transitionOrderStatus(orderId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("BR-007");

        verify(orderRepository, never()).save(any());
    }

    // ==========================================
    // API-ORD-006 & API-ORD-007: Stuck Orders Detection & Retry
    // ==========================================

    @Test
    @DisplayName("API-ORD-006 & API-ORD-007: Detect stuck orders and retry without altering state directly")
    void stuckOrders_DetectionAndRetry() {
        Order stuckOrder = new Order();
        stuckOrder.setStatus(OrderStatus.RESERVED);
        when(orderRepository.findByStatusAndPlacedAtBefore(eq(OrderStatus.RESERVED), any(Instant.class)))
                .thenReturn(List.of(stuckOrder));

        List<OrderResponse> stuck = orderService.getStuckOrders(60);
        assertThat(stuck).hasSize(1);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(stuckOrder));
        orderService.retryStuckOrder(orderId);

        // FR-042 / BR-011: re-publishes outbox event without force-changing status
        verify(outboxService).recordEvent(anyString(), eq("OrderStatusChanged"), any());
        assertThat(stuckOrder.getStatus()).isEqualTo(OrderStatus.RESERVED);
    }
}
