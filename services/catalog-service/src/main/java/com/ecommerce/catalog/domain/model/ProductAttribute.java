package com.ecommerce.catalog.domain.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "product_attribute")
public class ProductAttribute {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    public ProductAttribute() {
        this.id = UUID.randomUUID();
    }

    public ProductAttribute(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
