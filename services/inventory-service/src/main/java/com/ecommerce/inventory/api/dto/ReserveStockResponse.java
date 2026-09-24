package com.ecommerce.inventory.api.dto;

import java.util.List;
import java.util.UUID;

public class ReserveStockResponse {

    private UUID orderId;
    private List<UUID> reservationIds;
    private String status;

    public ReserveStockResponse() {
    }

    public ReserveStockResponse(UUID orderId, List<UUID> reservationIds, String status) {
        this.orderId = orderId;
        this.reservationIds = reservationIds;
        this.status = status;
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
}
