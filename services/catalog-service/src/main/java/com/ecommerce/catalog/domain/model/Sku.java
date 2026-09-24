package com.ecommerce.catalog.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "sku")
public class Sku {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "sku_code", nullable = false, unique = true)
    private String skuCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkuStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sku_attribute_value",
            joinColumns = @JoinColumn(name = "sku_id"),
            inverseJoinColumns = @JoinColumn(name = "attribute_value_id")
    )
    private Set<ProductAttributeValue> attributeValues = new HashSet<>();

    public Sku() {
        this.id = UUID.randomUUID();
        this.status = SkuStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public Sku(UUID productId, String skuCode) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.skuCode = skuCode;
        this.status = SkuStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public SkuStatus getStatus() {
        return status;
    }

    public void setStatus(SkuStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<ProductAttributeValue> getAttributeValues() {
        return attributeValues;
    }

    public void setAttributeValues(Set<ProductAttributeValue> attributeValues) {
        this.attributeValues = attributeValues;
    }
}
