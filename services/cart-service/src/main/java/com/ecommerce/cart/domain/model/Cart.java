package com.ecommerce.cart.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "cart")
public class Cart {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public Cart() {
        this.id = UUID.randomUUID();
        this.status = CartStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public Cart(UUID customerId) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.status = CartStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public CartStatus getStatus() {
        return status;
    }

    public void setStatus(CartStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public Optional<CartItem> findItemBySkuId(UUID skuId) {
        if (skuId == null) return Optional.empty();
        return items.stream().filter(item -> skuId.equals(item.getSkuId())).findFirst();
    }

    public Optional<CartItem> findItemById(UUID itemId) {
        if (itemId == null) return Optional.empty();
        return items.stream().filter(item -> itemId.equals(item.getId())).findFirst();
    }

    public void addItem(CartItem item) {
        if (item != null) {
            this.items.add(item);
            item.setCart(this);
            this.updatedAt = Instant.now();
        }
    }

    public void removeItem(CartItem item) {
        if (item != null) {
            this.items.remove(item);
            item.setCart(null);
            this.updatedAt = Instant.now();
        }
    }

    public void clearItems() {
        for (CartItem item : this.items) {
            item.setCart(null);
        }
        this.items.clear();
        this.updatedAt = Instant.now();
    }

    public void markAsCheckedOut() {
        this.status = CartStatus.CHECKED_OUT;
        this.updatedAt = Instant.now();
    }
}
