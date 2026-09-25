package com.ecommerce.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CreateSkuRequest {

    private UUID productId;

    @NotBlank(message = "SKU code is required")
    private String skuCode;

    private Set<UUID> attributeValueIds = new HashSet<>();

    public CreateSkuRequest() {
    }

    public CreateSkuRequest(UUID productId, String skuCode) {
        this.productId = productId;
        this.skuCode = skuCode;
        this.attributeValueIds = new HashSet<>();
    }

    public CreateSkuRequest(UUID productId, String skuCode, Set<UUID> attributeValueIds) {
        this.productId = productId;
        this.skuCode = skuCode;
        this.attributeValueIds = attributeValueIds != null ? attributeValueIds : new HashSet<>();
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

    public Set<UUID> getAttributeValueIds() {
        return attributeValueIds;
    }

    public void setAttributeValueIds(Set<UUID> attributeValueIds) {
        this.attributeValueIds = attributeValueIds != null ? attributeValueIds : new HashSet<>();
    }
}
