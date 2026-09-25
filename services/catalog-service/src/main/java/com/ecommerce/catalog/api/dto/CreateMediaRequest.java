package com.ecommerce.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class CreateMediaRequest {

    private UUID skuId;

    @NotBlank(message = "Media URL is required")
    private String url;

    @NotBlank(message = "Media type is required (e.g. IMAGE, VIDEO)")
    private String mediaType;

    private int sortOrder;

    public CreateMediaRequest() {
    }

    public CreateMediaRequest(UUID skuId, String url, String mediaType, int sortOrder) {
        this.skuId = skuId;
        this.url = url;
        this.mediaType = mediaType;
        this.sortOrder = sortOrder;
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
