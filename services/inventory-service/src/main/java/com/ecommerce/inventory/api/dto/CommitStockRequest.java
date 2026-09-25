package com.ecommerce.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CommitStockRequest {

    @NotNull(message = "Order ID is required")
    @JsonProperty("order_id")
    private UUID orderId;

    public CommitStockRequest() {
    }

    public CommitStockRequest(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }
}
