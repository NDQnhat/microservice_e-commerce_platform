package com.ecommerce.order.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreateOrderRequest {

    @JsonProperty("customer_id")
    private UUID customerId;

    @JsonProperty("cart_id")
    private UUID cartId;

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

    @JsonProperty("shipping_fee_amount")
    private BigDecimal shippingFeeAmount = BigDecimal.ZERO;

    @JsonProperty("currency")
    private String currency = "VND";

    @Valid
    @JsonProperty("items")
    private List<OrderItemRequest> items;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(UUID customerId, String shippingRecipientName, String shippingPhone,
                              String shippingLine1, String shippingLine2, String shippingWard,
                              String shippingDistrict, String shippingCity, BigDecimal shippingFeeAmount,
                              String currency, List<OrderItemRequest> items) {
        this.customerId = customerId;
        this.shippingRecipientName = shippingRecipientName;
        this.shippingPhone = shippingPhone;
        this.shippingLine1 = shippingLine1;
        this.shippingLine2 = shippingLine2;
        this.shippingWard = shippingWard;
        this.shippingDistrict = shippingDistrict;
        this.shippingCity = shippingCity;
        this.shippingFeeAmount = shippingFeeAmount != null ? shippingFeeAmount : BigDecimal.ZERO;
        this.currency = currency != null ? currency : "VND";
        this.items = items;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getCartId() {
        return cartId;
    }

    public void setCartId(UUID cartId) {
        this.cartId = cartId;
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

    public BigDecimal getShippingFeeAmount() {
        return shippingFeeAmount != null ? shippingFeeAmount : BigDecimal.ZERO;
    }

    public void setShippingFeeAmount(BigDecimal shippingFeeAmount) {
        this.shippingFeeAmount = shippingFeeAmount;
    }

    public String getCurrency() {
        return currency != null ? currency : "VND";
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<OrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
}
