package com.ecommerce.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class WarehouseStockDto {

    @JsonProperty("warehouse_id")
    private UUID warehouseId;

    @JsonProperty("warehouse_name")
    private String warehouseName;

    @JsonProperty("quantity_on_hand")
    private int quantityOnHand;

    @JsonProperty("quantity_available")
    private int quantityAvailable;

    public WarehouseStockDto() {
    }

    public WarehouseStockDto(UUID warehouseId, String warehouseName, int quantityOnHand, int quantityAvailable) {
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.quantityOnHand = quantityOnHand;
        this.quantityAvailable = quantityAvailable;
    }

    public UUID getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(UUID warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }
}
