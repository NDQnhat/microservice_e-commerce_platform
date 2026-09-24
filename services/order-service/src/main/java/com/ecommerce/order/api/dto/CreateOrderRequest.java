package com.ecommerce.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class CreateOrderRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotBlank(message = "Recipient name is required")
    private String shippingRecipientName;

    @NotBlank(message = "Phone is required")
    private String shippingPhone;

    @NotBlank(message = "Address line 1 is required")
    private String shippingLine1;

    private String shippingLine2;

    @NotBlank(message = "Ward is required")
    private String shippingWard;

    @NotBlank(message = "District is required")
    private String shippingDistrict;

    @NotBlank(message = "City is required")
    private String shippingCity;

    @NotNull(message = "Shipping fee is required")
    @DecimalMin(value = "0.00", message = "Shipping fee cannot be negative")
    private BigDecimal shippingFeeAmount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotEmpty(message = "Order must have at least one item")
    @Valid
    private List<OrderItemRequest> items;

    public CreateOrderRequest() {
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
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
        return shippingFeeAmount;
    }

    public void setShippingFeeAmount(BigDecimal shippingFeeAmount) {
        this.shippingFeeAmount = shippingFeeAmount;
    }

    public String getCurrency() {
        return currency;
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
