package com.ecommerce.order.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("customer_id")
    private UUID customerId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("idempotency_key")
    private String idempotencyKey;

    @JsonProperty("shipping_recipient_name")
    private String shippingRecipientName;

    @JsonProperty("shipping_phone")
    private String shippingPhone;

    @JsonProperty("shipping_line1")
    private String shippingLine1;

    @JsonProperty("shipping_line2")
    private String shippingLine2;

    @JsonProperty("shipping_ward")
    private String shippingWard;

    @JsonProperty("shipping_district")
    private String shippingDistrict;

    @JsonProperty("shipping_city")
    private String shippingCity;

    @JsonProperty("subtotal_amount")
    private BigDecimal subtotalAmount;

    @JsonProperty("shipping_fee_amount")
    private BigDecimal shippingFeeAmount;

    @JsonProperty("discount_amount")
    private BigDecimal discountAmount;

    @JsonProperty("grand_total_amount")
    private BigDecimal grandTotalAmount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("placed_at")
    private Instant placedAt;

    @JsonProperty("items")
    private List<OrderItemResponse> items;

    @JsonProperty("timeline")
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
