package com.ecommerce.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ReserveStockResponse {

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("reservation_ids")
    private List<UUID> reservationIds;

    @JsonProperty("status")
    private String status;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    public ReserveStockResponse() {
    }

    public ReserveStockResponse(UUID orderId, List<UUID> reservationIds, String status) {
        this.orderId = orderId;
        this.reservationIds = reservationIds;
        this.status = status;
        this.expiresAt = null;
    }

    public ReserveStockResponse(UUID orderId, List<UUID> reservationIds, String status, Instant expiresAt) {
        this.orderId = orderId;
        this.reservationIds = reservationIds;
        this.status = status;
        this.expiresAt = expiresAt;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public List<UUID> getReservationIds() {
        return reservationIds;
    }

    public void setReservationIds(List<UUID> reservationIds) {
        this.reservationIds = reservationIds;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
