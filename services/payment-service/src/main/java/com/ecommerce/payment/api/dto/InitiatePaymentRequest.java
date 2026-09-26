package com.ecommerce.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class InitiatePaymentRequest {

    @NotNull(message = "Order ID is required")
    @JsonProperty("order_id")
    @JsonAlias({"orderId", "order_id"})
    private UUID orderId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @JsonProperty("amount")
    @JsonAlias({"amount"})
    private BigDecimal amount;

    public InitiatePaymentRequest() {
    }

    public InitiatePaymentRequest(UUID orderId, BigDecimal amount) {
        this.orderId = orderId;
        this.amount = amount;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
