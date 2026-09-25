package com.ecommerce.catalog.api.dto;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SkuDetailDto {

    private UUID id;
    private UUID productId;
    private String skuCode;
    private String status;
    private Set<ProductAttributeValueDto> attributeValues = new HashSet<>();

    public SkuDetailDto() {
    }

    public SkuDetailDto(UUID id, UUID productId, String skuCode, String status, Set<ProductAttributeValueDto> attributeValues) {
        this.id = id;
        this.productId = productId;
        this.skuCode = skuCode;
        this.status = status;
        this.attributeValues = attributeValues != null ? attributeValues : new HashSet<>();
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

    public Set<ProductAttributeValueDto> getAttributeValues() {
        return attributeValues;
    }

    public void setAttributeValues(Set<ProductAttributeValueDto> attributeValues) {
        this.attributeValues = attributeValues != null ? attributeValues : new HashSet<>();
    }
}
