package com.ecommerce.order.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class ReleaseStockResponseDto {

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("reason")
    private String reason;

    public ReleaseStockResponseDto() {
    }

    public ReleaseStockResponseDto(UUID orderId, String status, String reason) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
