package com.ecommerce.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class CreateCategoryRequest {

    private UUID parentCategoryId;

    @NotBlank(message = "Category name is required")
    private String name;

    @NotBlank(message = "Slug is required")
    private String slug;

    public CreateCategoryRequest() {
    }

    public CreateCategoryRequest(UUID parentCategoryId, String name, String slug) {
        this.parentCategoryId = parentCategoryId;
        this.name = name;
        this.slug = slug;
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
}
