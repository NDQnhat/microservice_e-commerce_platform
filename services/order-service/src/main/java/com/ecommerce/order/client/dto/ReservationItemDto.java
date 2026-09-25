package com.ecommerce.order.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class ReservationItemDto {

    @JsonProperty("sku_id")
    private UUID skuId;

    @JsonProperty("quantity")
    private int quantity;

    public ReservationItemDto() {
    }

    public ReservationItemDto(UUID skuId, int quantity) {
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
