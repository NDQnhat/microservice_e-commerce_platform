package com.ecommerce.order.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public class CartDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("customer_id")
    private UUID customerId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("items")
    private List<CartItemDto> items;

    public CartDto() {
    }

    public CartDto(UUID id, UUID customerId, String status, List<CartItemDto> items) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.items = items;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<CartItemDto> getItems() {
        return items;
    }

    public void setItems(List<CartItemDto> items) {
        this.items = items;
    }
}
