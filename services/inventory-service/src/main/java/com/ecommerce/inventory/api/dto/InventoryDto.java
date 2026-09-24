package com.ecommerce.inventory.api.dto;

import java.util.UUID;

public class InventoryDto {

    private UUID skuId;
    private int quantityOnHand;
    private int quantityReserved;
    private int quantityAvailable;

    public InventoryDto() {
    }

    public InventoryDto(UUID skuId, int quantityOnHand, int quantityReserved, int quantityAvailable) {
        this.skuId = skuId;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.quantityAvailable = quantityAvailable;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public void setSkuId(UUID skuId) {
        this.skuId = skuId;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(int quantityReserved) {
        this.quantityReserved = quantityReserved;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }
}
