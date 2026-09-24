package com.ecommerce.catalog.api.dto;

import java.util.UUID;

public class CategoryDto {

    private UUID id;
    private UUID parentCategoryId;
    private String name;
    private String slug;
    private String status;

    public CategoryDto() {
    }

    public CategoryDto(UUID id, UUID parentCategoryId, String name, String slug, String status) {
        this.id = id;
        this.parentCategoryId = parentCategoryId;
        this.name = name;
        this.slug = slug;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getParentCategoryId() {
        return parentCategoryId;
    }

    public void setParentCategoryId(UUID parentCategoryId) {
        this.parentCategoryId = parentCategoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
