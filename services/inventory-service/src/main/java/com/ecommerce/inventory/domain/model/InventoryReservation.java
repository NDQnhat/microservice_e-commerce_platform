package com.ecommerce.inventory.domain.model;

import com.ecommerce.common.error.InvalidStateException;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_reservation")
public class InventoryReservation {

    @Id
    private UUID id;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public InventoryReservation() {
        this.id = UUID.randomUUID();
        this.status = ReservationStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public InventoryReservation(UUID skuId, UUID orderId, int quantity, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.skuId = skuId;
        this.orderId = orderId;
        this.quantity = quantity;
        this.status = ReservationStatus.ACTIVE;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void consume() {
        if (!this.status.canTransitionTo(ReservationStatus.CONSUMED)) {
            throw new InvalidStateException("Cannot transition reservation from " + this.status + " to CONSUMED");
        }
        this.status = ReservationStatus.CONSUMED;
    }

    public void release() {
        if (!this.status.canTransitionTo(ReservationStatus.RELEASED)) {
            throw new InvalidStateException("Cannot transition reservation from " + this.status + " to RELEASED");
        }
        this.status = ReservationStatus.RELEASED;
    }

    public void expire() {
        if (!this.status.canTransitionTo(ReservationStatus.EXPIRED)) {
            throw new InvalidStateException("Cannot transition reservation from " + this.status + " to EXPIRED");
        }
        this.status = ReservationStatus.EXPIRED;
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

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
