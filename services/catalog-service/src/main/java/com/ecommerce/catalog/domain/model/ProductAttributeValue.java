package com.ecommerce.catalog.domain.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "product_attribute_value")
public class ProductAttributeValue {

    @Id
    private UUID id;

    @Column(name = "attribute_id", nullable = false)
    private UUID attributeId;

    @Column(nullable = false)
    private String value;

    public ProductAttributeValue() {
        this.id = UUID.randomUUID();
    }

    public ProductAttributeValue(UUID attributeId, String value) {
        this.id = UUID.randomUUID();
        this.attributeId = attributeId;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(UUID attributeId) {
        this.attributeId = attributeId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
