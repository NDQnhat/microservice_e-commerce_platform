package com.ecommerce.order.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "idempotency_key", nullable = false, unique = true, updatable = false)
    private String idempotencyKey;

    @Column(name = "shipping_recipient_name", nullable = false, updatable = false)
    private String shippingRecipientName;

    @Column(name = "shipping_phone", nullable = false, updatable = false)
    private String shippingPhone;

    @Column(name = "shipping_line1", nullable = false, updatable = false)
    private String shippingLine1;

    @Column(name = "shipping_line2", updatable = false)
    private String shippingLine2;

    @Column(name = "shipping_ward", nullable = false, updatable = false)
    private String shippingWard;

    @Column(name = "shipping_district", nullable = false, updatable = false)
    private String shippingDistrict;

    @Column(name = "shipping_city", nullable = false, updatable = false)
    private String shippingCity;

    @Column(name = "subtotal_amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal subtotalAmount;

    @Column(name = "shipping_fee_amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal shippingFeeAmount;

    @Column(name = "discount_amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal discountAmount;

    @Column(name = "grand_total_amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal grandTotalAmount;

    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderTimelineEvent> timelineEvents = new ArrayList<>();

    public Order() {
        this.id = UUID.randomUUID();
        this.status = OrderStatus.RESERVED;
        this.placedAt = Instant.now();
    }

    public Order(UUID customerId, String idempotencyKey,
                 String shippingRecipientName, String shippingPhone,
                 String shippingLine1, String shippingLine2,
                 String shippingWard, String shippingDistrict, String shippingCity,
                 BigDecimal subtotalAmount, BigDecimal shippingFeeAmount,
                 BigDecimal discountAmount, BigDecimal grandTotalAmount,
                 String currency) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.status = OrderStatus.RESERVED;
        this.idempotencyKey = idempotencyKey;
        this.shippingRecipientName = shippingRecipientName;
        this.shippingPhone = shippingPhone;
        this.shippingLine1 = shippingLine1;
        this.shippingLine2 = shippingLine2;
        this.shippingWard = shippingWard;
        this.shippingDistrict = shippingDistrict;
        this.shippingCity = shippingCity;
        this.subtotalAmount = subtotalAmount;
        this.shippingFeeAmount = shippingFeeAmount;
        this.discountAmount = discountAmount;
        this.grandTotalAmount = grandTotalAmount;
        this.currency = currency;
        this.placedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getShippingRecipientName() {
        return shippingRecipientName;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public String getShippingLine1() {
        return shippingLine1;
    }

    public String getShippingLine2() {
        return shippingLine2;
    }

    public String getShippingWard() {
        return shippingWard;
    }

    public String getShippingDistrict() {
        return shippingDistrict;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public BigDecimal getShippingFeeAmount() {
        return shippingFeeAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getGrandTotalAmount() {
        return grandTotalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public List<OrderTimelineEvent> getTimelineEvents() {
        return timelineEvents;
    }

    public void setTimelineEvents(List<OrderTimelineEvent> timelineEvents) {
        this.timelineEvents = timelineEvents;
    }

    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void addTimelineEvent(OrderTimelineEvent event) {
        timelineEvents.add(event);
    }
}
