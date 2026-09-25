package com.ecommerce.order.service;

import com.ecommerce.common.error.AuthorizationFailedException;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.order.api.dto.*;
import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.client.InventoryClient;
import com.ecommerce.order.client.PricingClient;
import com.ecommerce.order.client.dto.CartDto;
import com.ecommerce.order.client.dto.PriceResponseDto;
import com.ecommerce.order.client.dto.ReservationItemDto;
import com.ecommerce.order.domain.model.*;
import com.ecommerce.order.domain.repository.OrderItemRepository;
import com.ecommerce.order.domain.repository.OrderRepository;
import com.ecommerce.order.domain.repository.OrderTimelineEventRepository;
import com.ecommerce.order.domain.statemachine.OrderStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderTimelineEventRepository timelineRepository;
    private final OrderStateMachine stateMachine;
    private final OrderOutboxService outboxService;
    private final InventoryClient inventoryClient;
    private final PricingClient pricingClient;
    private final CartClient cartClient;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        OrderTimelineEventRepository timelineRepository,
                        OrderStateMachine stateMachine,
                        OrderOutboxService outboxService,
                        InventoryClient inventoryClient,
                        PricingClient pricingClient,
                        CartClient cartClient) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.timelineRepository = timelineRepository;
        this.stateMachine = stateMachine;
        this.outboxService = outboxService;
        this.inventoryClient = inventoryClient;
        this.pricingClient = pricingClient;
        this.cartClient = cartClient;
    }

    // ==========================================
    // API-ORD-001: Checkout / Create Order
    // ==========================================
    public CreateOrderResult createOrder(UUID customerId, String idempotencyKey, CreateOrderRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required per NFR-IDEMPOTENCY-001");
        }

        // BR-010: Idempotency Replay Check
        Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOrder.isPresent()) {
            Order existing = existingOrder.get();
            if (customerId != null && !existing.getCustomerId().equals(customerId)) {
                throw new BusinessRuleException("BR-010", "Idempotency key already used for a different customer");
            }
            log.info("Idempotent checkout replay for key: {}, order: {}", idempotencyKey, existing.getId());
            return new CreateOrderResult(mapToResponse(existing), true);
        }

        // BR-009: Guest checkout prohibited
        UUID targetCustomerId = customerId != null ? customerId : (request != null ? request.getCustomerId() : null);
        if (targetCustomerId == null) {
            throw new BusinessRuleException("BR-009", "Checkout requires an authenticated customer session");
        }

        // Resolve order items: explicit or from customer cart
        List<OrderItemRequest> rawItems = (request != null && request.getItems() != null && !request.getItems().isEmpty())
                ? request.getItems()
                : resolveItemsFromCart(targetCustomerId);

        if (rawItems == null || rawItems.isEmpty()) {
            throw new BusinessRuleException("BR-004", "Cart or order items list cannot be empty");
        }

        // Immutable Pricing Snapshot & Breakdown computation (BR-013, FR-027, FR-028)
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItemRequest> processedItems = new ArrayList<>();

        for (OrderItemRequest item : rawItems) {
            if (item.getQuantity() <= 0) {
                throw new BusinessRuleException("BR-004", "Quantity must be greater than zero");
            }

            BigDecimal unitPrice = item.getUnitPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
                PriceResponseDto priceDto = pricingClient.getEffectivePrice(item.getSkuId());
                if (priceDto != null && priceDto.getEffectivePrice() != null) {
                    unitPrice = priceDto.getEffectivePrice();
                } else {
                    unitPrice = new BigDecimal("100000.00");
                }
            }

            String productName = item.getProductName() != null && !item.getProductName().isBlank()
                    ? item.getProductName()
                    : "Product " + item.getSkuId().toString().substring(0, 8);
            String skuCode = item.getSkuCode() != null && !item.getSkuCode().isBlank()
                    ? item.getSkuCode()
                    : "SKU-" + item.getSkuId().toString().substring(0, 8);
            String attributeSnapshot = item.getAttributeSnapshot() != null && !item.getAttributeSnapshot().isBlank()
                    ? item.getAttributeSnapshot()
                    : "Default";

            OrderItemRequest processed = new OrderItemRequest(item.getSkuId(), productName, skuCode, attributeSnapshot, unitPrice, item.getQuantity());
            processedItems.add(processed);

            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal shippingFee = (request != null && request.getShippingFeeAmount() != null)
                ? request.getShippingFeeAmount()
                : BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO; // ASM-008: 0 in MVP
        BigDecimal grandTotal = subtotal.add(shippingFee).subtract(discount);
        String currency = (request != null && request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency()
                : "VND";

        UUID orderId = UUID.randomUUID();

        // FR-019 / BR-004: Synchronous Inventory Reservation
        List<ReservationItemDto> reservationItems = processedItems.stream()
                .map(i -> new ReservationItemDto(i.getSkuId(), i.getQuantity()))
                .collect(Collectors.toList());
        inventoryClient.reserveStock(orderId, reservationItems, 15);

        // Address Snapshot (BR-017)
        String recipientName = (request != null && request.getShippingRecipientName() != null && !request.getShippingRecipientName().isBlank())
                ? request.getShippingRecipientName()
                : "Valued Customer";
        String phone = (request != null && request.getShippingPhone() != null && !request.getShippingPhone().isBlank())
                ? request.getShippingPhone()
                : "0900000000";
        String line1 = (request != null && request.getShippingLine1() != null && !request.getShippingLine1().isBlank())
                ? request.getShippingLine1()
                : "123 Main Street";
        String line2 = request != null ? request.getShippingLine2() : null;
        String ward = (request != null && request.getShippingWard() != null && !request.getShippingWard().isBlank())
                ? request.getShippingWard()
                : "Ward 1";
        String district = (request != null && request.getShippingDistrict() != null && !request.getShippingDistrict().isBlank())
                ? request.getShippingDistrict()
                : "District 1";
        String city = (request != null && request.getShippingCity() != null && !request.getShippingCity().isBlank())
                ? request.getShippingCity()
                : "Ho Chi Minh City";

        Order order = new Order(
                targetCustomerId,
                idempotencyKey,
                recipientName,
                phone,
                line1,
                line2,
                ward,
                district,
                city,
                subtotal,
                shippingFee,
                discount,
                grandTotal,
                currency
        );
        order.setId(orderId);

        Order savedOrder = orderRepository.save(order);

        for (OrderItemRequest item : processedItems) {
            OrderItem orderItem = new OrderItem(
                    savedOrder,
                    item.getSkuId(),
                    item.getProductName(),
                    item.getSkuCode(),
                    item.getAttributeSnapshot(),
                    item.getUnitPrice(),
                    item.getQuantity()
            );
            orderItemRepository.save(orderItem);
            savedOrder.addItem(orderItem);
        }

        // BR-008: Record initial timeline event (null -> RESERVED)
        OrderTimelineEvent initialTimeline = new OrderTimelineEvent(
                savedOrder,
                null,
                OrderStatus.RESERVED,
                targetCustomerId,
                TimelineActorType.CUSTOMER,
                "Order created via checkout"
        );
        timelineRepository.save(initialTimeline);
        savedOrder.addTimelineEvent(initialTimeline);

        // NFR-OUTBOX-001: Record OrderCreated outbox event
        outboxService.recordEvent(savedOrder.getId().toString(), "OrderCreated", mapToResponse(savedOrder));

        // Clear cart if checkout was from cart
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            cartClient.clearCart(targetCustomerId);
        }

        return new CreateOrderResult(mapToResponse(savedOrder), false);
    }

    private List<OrderItemRequest> resolveItemsFromCart(UUID customerId) {
        CartDto cart = cartClient.getCart(customerId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return Collections.emptyList();
        }
        return cart.getItems().stream()
                .map(ci -> new OrderItemRequest(ci.getSkuId(), ci.getQuantity()))
                .collect(Collectors.toList());
    }

    // ==========================================
    // API-ORD-002: Get Order Detail & Timeline
    // ==========================================
    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId, UUID customerId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (!isAdmin && (customerId == null || !order.getCustomerId().equals(customerId))) {
            throw new AuthorizationFailedException("Access denied: cannot access another customer's order");
        }

        return mapToResponse(order);
    }

    // ==========================================
    // API-ORD-003: Cancel Order (Customer/Admin)
    // ==========================================
    public OrderResponse cancelOrder(UUID orderId, UUID customerId, boolean isAdmin, CancelOrderRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (!isAdmin && (customerId == null || !order.getCustomerId().equals(customerId))) {
            throw new AuthorizationFailedException("Access denied: cannot cancel another customer's order");
        }

        // Validate state machine transition (BR-001, BR-006, BR-007)
        stateMachine.validateTransition(order.getStatus(), OrderStatus.CANCELLED);

        OrderStatus fromStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        Order updated = orderRepository.save(order);

        // FR-020: Compensation - release inventory reservation
        String reason = request != null && request.getReason() != null ? request.getReason() : "Cancelled by user";
        inventoryClient.releaseStock(orderId, reason);

        // BR-008: Record cancellation timeline event
        TimelineActorType actorType = isAdmin ? TimelineActorType.BACK_OFFICE : TimelineActorType.CUSTOMER;
        OrderTimelineEvent cancelTimeline = new OrderTimelineEvent(
                updated,
                fromStatus,
                OrderStatus.CANCELLED,
                customerId,
                actorType,
                "Cancelled: " + reason
        );
        timelineRepository.save(cancelTimeline);
        updated.addTimelineEvent(cancelTimeline);

        // NFR-OUTBOX-001: Record OrderCancelled outbox event
        outboxService.recordEvent(updated.getId().toString(), "OrderCancelled", mapToResponse(updated));

        return mapToResponse(updated);
    }

    // ==========================================
    // API-ORD-004: List Orders (Backoffice)
    // ==========================================
    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(OrderStatus status, UUID customerId, Pageable pageable) {
        Page<Order> page;
        if (status != null && customerId != null) {
            page = orderRepository.findByCustomerIdAndStatusOrderByPlacedAtDesc(customerId, status, pageable);
        } else if (status != null) {
            page = orderRepository.findByStatusOrderByPlacedAtDesc(status, pageable);
        } else if (customerId != null) {
            page = orderRepository.findByCustomerIdOrderByPlacedAtDesc(customerId, pageable);
        } else {
            page = orderRepository.findAllByOrderByPlacedAtDesc(pageable);
        }
        return page.map(this::mapToResponse);
    }

    // ==========================================
    // API-ORD-005: State Machine Transition (Backoffice)
    // ==========================================
    public OrderResponse transitionOrderStatus(UUID orderId, OrderTransitionRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        if (request == null || request.getTargetStatus() == null) {
            throw new IllegalArgumentException("Target status is required");
        }

        // BR-011: Strict state machine transition validation
        stateMachine.validateTransition(order.getStatus(), request.getTargetStatus());

        OrderStatus fromStatus = order.getStatus();
        order.setStatus(request.getTargetStatus());
        Order saved = orderRepository.save(order);

        // If target is CANCELLED: trigger compensation
        if (request.getTargetStatus() == OrderStatus.CANCELLED) {
            inventoryClient.releaseStock(orderId, request.getNote() != null ? request.getNote() : "Admin cancelled");
        }

        // BR-008: Record transition timeline event
        OrderTimelineEvent timelineEvent = new OrderTimelineEvent(
                saved,
                fromStatus,
                request.getTargetStatus(),
                request.getActorId(),
                TimelineActorType.BACK_OFFICE,
                request.getNote() != null ? request.getNote() : "Status updated by admin"
        );
        timelineRepository.save(timelineEvent);
        saved.addTimelineEvent(timelineEvent);

        // NFR-OUTBOX-001: Record outbox event
        String eventType = request.getTargetStatus() == OrderStatus.CANCELLED ? "OrderCancelled" : "OrderStatusChanged";
        outboxService.recordEvent(saved.getId().toString(), eventType, mapToResponse(saved));

        return mapToResponse(saved);
    }

    // ==========================================
    // API-ORD-006 & API-ORD-007: Stuck Orders Detection & Retry
    // ==========================================
    @Transactional(readOnly = true)
    public List<OrderResponse> getStuckOrders(int thresholdMinutes) {
        Instant cutoff = Instant.now().minus(thresholdMinutes, ChronoUnit.MINUTES);
        List<Order> stuck = orderRepository.findByStatusAndPlacedAtBefore(OrderStatus.RESERVED, cutoff);
        return stuck.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public void retryStuckOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));

        // FR-042 / BR-011: re-publishes event, does NOT force status directly
        outboxService.recordEvent(order.getId().toString(), "OrderStatusChanged", mapToResponse(order));
    }

    // ==========================================
    // Mapping Helpers
    // ==========================================
    public OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setStatus(order.getStatus().name());
        response.setIdempotencyKey(order.getIdempotencyKey());
        response.setShippingRecipientName(order.getShippingRecipientName());
        response.setShippingPhone(order.getShippingPhone());
        response.setShippingLine1(order.getShippingLine1());
        response.setShippingLine2(order.getShippingLine2());
        response.setShippingWard(order.getShippingWard());
        response.setShippingDistrict(order.getShippingDistrict());
        response.setShippingCity(order.getShippingCity());
        response.setSubtotalAmount(order.getSubtotalAmount());
        response.setShippingFeeAmount(order.getShippingFeeAmount());
        response.setDiscountAmount(order.getDiscountAmount());
        response.setGrandTotalAmount(order.getGrandTotalAmount());
        response.setCurrency(order.getCurrency());
        response.setPlacedAt(order.getPlacedAt());

        List<OrderItem> items = (order.getItems() != null && !order.getItems().isEmpty())
                ? order.getItems()
                : orderItemRepository.findByOrderId(order.getId());

        response.setItems(items.stream().map(i -> new OrderItemResponse(
                i.getId(),
                i.getSkuId(),
                i.getProductNameSnapshot(),
                i.getSkuCodeSnapshot(),
                i.getAttributeSnapshot(),
                i.getUnitPriceSnapshot(),
                i.getQuantity(),
                i.getLineTotal()
        )).collect(Collectors.toList()));

        List<OrderTimelineEvent> events = (order.getTimelineEvents() != null && !order.getTimelineEvents().isEmpty())
                ? order.getTimelineEvents()
                : timelineRepository.findByOrderIdOrderByOccurredAtAsc(order.getId());

        response.setTimeline(events.stream().map(t -> new OrderTimelineEventResponse(
                t.getId(),
                t.getOrder() != null ? t.getOrder().getId() : order.getId(),
                t.getFromStatus() != null ? t.getFromStatus().name() : null,
                t.getToStatus().name(),
                t.getActorId(),
                t.getActorType().name(),
                t.getNote(),
                t.getOccurredAt()
        )).collect(Collectors.toList()));

        return response;
    }
}
