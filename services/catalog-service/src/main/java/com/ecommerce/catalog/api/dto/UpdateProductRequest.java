package com.ecommerce.catalog.api.dto;

import java.util.UUID;

public class UpdateProductRequest {

    private UUID categoryId;
    private String name;
    private String description;
    private String status;

    public UpdateProductRequest() {
    }

    public UpdateProductRequest(UUID categoryId, String name, String description, String status) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.status = status;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
