package com.ecommerce.order.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderResponse {

    private UUID id;
    private UUID customerId;
    private String status;
    private String idempotencyKey;
    private String shippingRecipientName;
    private String shippingPhone;
    private String shippingLine1;
    private String shippingLine2;
    private String shippingWard;
    private String shippingDistrict;
    private String shippingCity;
    private BigDecimal subtotalAmount;
    private BigDecimal shippingFeeAmount;
    private BigDecimal discountAmount;
    private BigDecimal grandTotalAmount;
    private String currency;
    private Instant placedAt;
    private List<OrderItemResponse> items;
    private List<OrderTimelineEventResponse> timeline;

    public OrderResponse() {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getShippingRecipientName() {
        return shippingRecipientName;
    }

    public void setShippingRecipientName(String shippingRecipientName) {
        this.shippingRecipientName = shippingRecipientName;
    }

    public String getShippingPhone() {
        return shippingPhone;
    }

    public void setShippingPhone(String shippingPhone) {
        this.shippingPhone = shippingPhone;
    }

    public String getShippingLine1() {
        return shippingLine1;
    }

    public void setShippingLine1(String shippingLine1) {
        this.shippingLine1 = shippingLine1;
    }

    public String getShippingLine2() {
        return shippingLine2;
    }

    public void setShippingLine2(String shippingLine2) {
        this.shippingLine2 = shippingLine2;
    }

    public String getShippingWard() {
        return shippingWard;
    }

    public void setShippingWard(String shippingWard) {
        this.shippingWard = shippingWard;
    }

    public String getShippingDistrict() {
        return shippingDistrict;
    }

    public void setShippingDistrict(String shippingDistrict) {
        this.shippingDistrict = shippingDistrict;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }

    public BigDecimal getShippingFeeAmount() {
        return shippingFeeAmount;
    }

    public void setShippingFeeAmount(BigDecimal shippingFeeAmount) {
        this.shippingFeeAmount = shippingFeeAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getGrandTotalAmount() {
        return grandTotalAmount;
    }

    public void setGrandTotalAmount(BigDecimal grandTotalAmount) {
        this.grandTotalAmount = grandTotalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(Instant placedAt) {
        this.placedAt = placedAt;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderItemResponse> items) {
        this.items = items;
    }

    public List<OrderTimelineEventResponse> getTimeline() {
        return timeline;
    }

    public void setTimeline(List<OrderTimelineEventResponse> timeline) {
        this.timeline = timeline;
    }
}
