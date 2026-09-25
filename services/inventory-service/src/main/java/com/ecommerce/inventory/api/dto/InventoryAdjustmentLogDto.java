package com.ecommerce.inventory.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public class InventoryAdjustmentLogDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("sku_id")
    private UUID skuId;

    @JsonProperty("actor_id")
    private UUID actorId;

    @JsonProperty("quantity_before")
    private int quantityBefore;

    @JsonProperty("quantity_after")
    private int quantityAfter;

    @JsonProperty("delta")
    private int delta;

    @JsonProperty("reason_code")
    private String reasonCode;

    @JsonProperty("note")
    private String note;

    @JsonProperty("created_at")
    private Instant createdAt;

    public InventoryAdjustmentLogDto() {
    }

    public InventoryAdjustmentLogDto(UUID id, UUID skuId, UUID actorId, int quantityBefore,
                                    int quantityAfter, int delta, String reasonCode,
                                    String note, Instant createdAt) {
        this.id = id;
        this.skuId = skuId;
        this.actorId = actorId;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.delta = delta;
        this.reasonCode = reasonCode;
        this.note = note;
        this.createdAt = createdAt;
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

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public int getQuantityBefore() {
        return quantityBefore;
    }

    public void setQuantityBefore(int quantityBefore) {
        this.quantityBefore = quantityBefore;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(int quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(String reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
