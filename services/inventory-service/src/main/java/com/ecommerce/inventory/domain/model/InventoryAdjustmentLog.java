package com.ecommerce.inventory.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_adjustment_log")
public class InventoryAdjustmentLog {

    @Id
    private UUID id;

    @Column(name = "sku_id", nullable = false, updatable = false)
    private UUID skuId;

    @Column(name = "actor_id", nullable = false, updatable = false)
    private UUID actorId;

    @Column(name = "quantity_before", nullable = false, updatable = false)
    private int quantityBefore;

    @Column(name = "quantity_after", nullable = false, updatable = false)
    private int quantityAfter;

    @Column(nullable = false, updatable = false)
    private int delta;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, updatable = false)
    private AdjustmentReasonCode reasonCode;

    @Column(columnDefinition = "text", updatable = false)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public InventoryAdjustmentLog() {
        this.id = UUID.randomUUID();
        this.createdAt = Instant.now();
    }

    public InventoryAdjustmentLog(UUID skuId, UUID actorId, int quantityBefore,
                                  int quantityAfter, int delta,
                                  AdjustmentReasonCode reasonCode, String note) {
        this.id = UUID.randomUUID();
        this.skuId = skuId;
        this.actorId = actorId;
        this.quantityBefore = quantityBefore;
        this.quantityAfter = quantityAfter;
        this.delta = delta;
        this.reasonCode = reasonCode;
        this.note = note;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public int getQuantityBefore() {
        return quantityBefore;
    }

    public int getQuantityAfter() {
        return quantityAfter;
    }

    public int getDelta() {
        return delta;
    }

    public AdjustmentReasonCode getReasonCode() {
        return reasonCode;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
