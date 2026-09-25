package com.ecommerce.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ReleaseStockRequest {

    @NotNull(message = "Order ID is required")
    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("reason")
    private String reason;

    public ReleaseStockRequest() {
    }

    public ReleaseStockRequest(UUID orderId) {
        this.orderId = orderId;
        this.reason = "PAYMENT_FAILED_OR_ORDER_CANCELLED";
    }

    public ReleaseStockRequest(UUID orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
