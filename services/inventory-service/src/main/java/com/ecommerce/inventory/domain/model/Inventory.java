package com.ecommerce.inventory.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    private UUID id;

    @Column(name = "sku_id", nullable = false, unique = true)
    private UUID skuId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand;

    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Inventory() {
        this.id = UUID.randomUUID();
        this.quantityOnHand = 0;
        this.quantityReserved = 0;
        this.version = 0L;
        this.updatedAt = Instant.now();
    }

    public Inventory(UUID skuId, int quantityOnHand) {
        this.id = UUID.randomUUID();
        this.skuId = skuId;
        this.quantityOnHand = quantityOnHand;
        this.quantityReserved = 0;
        this.version = 0L;
        this.updatedAt = Instant.now();
    }

    public int getQuantityAvailable() {
        return this.quantityOnHand - this.quantityReserved;
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

    public int getQuantityOnHand() {
        return quantityOnHand;
    }

    public void setQuantityOnHand(int quantityOnHand) {
        this.quantityOnHand = quantityOnHand;
        this.updatedAt = Instant.now();
    }

    public int getQuantityReserved() {
        return quantityReserved;
    }

    public void setQuantityReserved(int quantityReserved) {
        this.quantityReserved = quantityReserved;
        this.updatedAt = Instant.now();
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
