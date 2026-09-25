package com.ecommerce.cart.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class CartItemDto {

    private UUID id;

    @JsonProperty("sku_id")
    private UUID skuId;

    private int quantity;

    public CartItemDto() {
    }

    public CartItemDto(UUID id, UUID skuId, int quantity) {
        this.id = id;
        this.skuId = skuId;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
