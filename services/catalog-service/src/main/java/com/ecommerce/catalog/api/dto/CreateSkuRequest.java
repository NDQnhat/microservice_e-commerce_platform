package com.ecommerce.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreateSkuRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotBlank(message = "SKU code is required")
    private String skuCode;

    public CreateSkuRequest() {
    }

    public CreateSkuRequest(UUID productId, String skuCode) {
        this.productId = productId;
        this.skuCode = skuCode;
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
}
