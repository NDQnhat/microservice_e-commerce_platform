package com.ecommerce.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InventoryDto {

    @JsonProperty("sku_id")
    private UUID skuId;

    @JsonProperty("quantity_on_hand")
    private int quantityOnHand;

    @JsonProperty("quantity_reserved")
    private int quantityReserved;

    @JsonProperty("quantity_available")
    private int quantityAvailable;

    @JsonProperty("warehouses")
    private List<WarehouseStockDto> warehouses = new ArrayList<>();

    public InventoryDto() {
    }

    public InventoryDto(UUID skuId, int quantityOnHand, int quantityReserved, int quantityAvailable) {
        this.skuId = skuId;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.quantityAvailable = quantityAvailable;
        this.warehouses = List.of(
                new WarehouseStockDto(
                        UUID.nameUUIDFromBytes("main-warehouse".getBytes()),
                        "Primary Distribution Center",
                        quantityOnHand,
                        quantityAvailable
                )
        );
    }

    public InventoryDto(UUID skuId, int quantityOnHand, int quantityReserved, int quantityAvailable, List<WarehouseStockDto> warehouses) {
        this.skuId = skuId;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = quantityReserved;
        this.quantityAvailable = quantityAvailable;
        this.warehouses = warehouses != null ? warehouses : new ArrayList<>();
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

    public List<WarehouseStockDto> getWarehouses() {
        return warehouses;
    }

    public void setWarehouses(List<WarehouseStockDto> warehouses) {
        this.warehouses = warehouses;
    }
}
