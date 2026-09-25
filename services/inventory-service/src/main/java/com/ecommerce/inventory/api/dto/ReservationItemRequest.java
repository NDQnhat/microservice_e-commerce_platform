package com.ecommerce.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class ReservationItemRequest {

    @NotNull(message = "SKU ID is required")
    @JsonProperty("sku_id")
    private UUID skuId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @JsonProperty("quantity")
    private int quantity;

    public ReservationItemRequest() {
    }

    public ReservationItemRequest(UUID skuId, int quantity) {
        this.skuId = skuId;
        this.quantity = quantity;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public void setSkuId(UUID skuId) {
        this.skuId = skuId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
