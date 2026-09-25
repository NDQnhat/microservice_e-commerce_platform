package com.ecommerce.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class ReserveStockRequest {

    @NotNull(message = "Order ID is required")
    @JsonProperty("order_id")
    private UUID orderId;

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    @JsonProperty("items")
    private List<ReservationItemRequest> items;

    @JsonProperty("ttl_minutes")
    private Integer ttlMinutes;

    public ReserveStockRequest() {
    }

    public ReserveStockRequest(UUID orderId, List<ReservationItemRequest> items) {
        this.orderId = orderId;
        this.items = items;
        this.ttlMinutes = 15;
    }

    public ReserveStockRequest(UUID orderId, List<ReservationItemRequest> items, Integer ttlMinutes) {
        this.orderId = orderId;
        this.items = items;
        this.ttlMinutes = ttlMinutes != null ? ttlMinutes : 15;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public List<ReservationItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ReservationItemRequest> items) {
        this.items = items;
    }

    public Integer getTtlMinutes() {
        return ttlMinutes != null ? ttlMinutes : 15;
    }

    public void setTtlMinutes(Integer ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }
}
