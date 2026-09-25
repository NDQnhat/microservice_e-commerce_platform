package com.ecommerce.catalog.api.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductDetailDto {

    private UUID id;
    private UUID categoryId;
    private String name;
    private String description;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<SkuDetailDto> skus = new ArrayList<>();
    private List<ProductMediaDto> media = new ArrayList<>();

    public ProductDetailDto() {
    }

    public ProductDetailDto(UUID id, UUID categoryId, String name, String description, String status,
                            Instant createdAt, Instant updatedAt,
                            List<SkuDetailDto> skus, List<ProductMediaDto> media) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.skus = skus != null ? skus : new ArrayList<>();
        this.media = media != null ? media : new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<SkuDetailDto> getSkus() {
        return skus;
    }

    public void setSkus(List<SkuDetailDto> skus) {
        this.skus = skus != null ? skus : new ArrayList<>();
    }

    public List<ProductMediaDto> getMedia() {
        return media;
    }

    public void setMedia(List<ProductMediaDto> media) {
        this.media = media != null ? media : new ArrayList<>();
    }
}
