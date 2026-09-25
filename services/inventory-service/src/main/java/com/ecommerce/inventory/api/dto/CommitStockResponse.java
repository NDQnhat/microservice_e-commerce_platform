package com.ecommerce.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class CommitStockResponse {

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("status")
    private String status;

    public CommitStockResponse() {
    }

    public CommitStockResponse(UUID orderId, String status) {
        this.orderId = orderId;
        this.status = status;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
