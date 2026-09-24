package com.ecommerce.catalog.api.dto;

import java.util.UUID;

public class ProductMediaDto {

    private UUID id;
    private UUID productId;
    private UUID skuId;
    private String url;
    private String mediaType;
    private int sortOrder;

    public ProductMediaDto() {
    }

    public ProductMediaDto(UUID id, UUID productId, UUID skuId, String url, String mediaType, int sortOrder) {
        this.id = id;
        this.productId = productId;
        this.skuId = skuId;
        this.url = url;
        this.mediaType = mediaType;
        this.sortOrder = sortOrder;
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

    public UUID getSkuId() {
        return skuId;
    }

    public void setSkuId(UUID skuId) {
        this.skuId = skuId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
