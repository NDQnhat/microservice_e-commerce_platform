package com.ecommerce.order.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public class ReserveStockRequestDto {

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("items")
    private List<ReservationItemDto> items;

    @JsonProperty("ttl_minutes")
    private Integer ttlMinutes;

    public ReserveStockRequestDto() {
    }

    public ReserveStockRequestDto(UUID orderId, List<ReservationItemDto> items) {
        this.orderId = orderId;
        this.items = items;
        this.ttlMinutes = 15;
    }

    public ReserveStockRequestDto(UUID orderId, List<ReservationItemDto> items, Integer ttlMinutes) {
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

    public List<ReservationItemDto> getItems() {
        return items;
    }

    public void setItems(List<ReservationItemDto> items) {
        this.items = items;
    }

    public Integer getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(Integer ttlMinutes) {
        this.ttlMinutes = ttlMinutes;
    }
}
