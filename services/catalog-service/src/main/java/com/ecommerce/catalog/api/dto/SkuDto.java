package com.ecommerce.catalog.api.dto;

import java.util.UUID;

public class SkuDto {

    private UUID id;
    private UUID productId;
    private String skuCode;
    private String status;

    public SkuDto() {
    }

    public SkuDto(UUID id, UUID productId, String skuCode, String status) {
        this.id = id;
        this.productId = productId;
        this.skuCode = skuCode;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
