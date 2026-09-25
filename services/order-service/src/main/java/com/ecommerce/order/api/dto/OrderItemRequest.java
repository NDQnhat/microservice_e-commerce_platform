package com.ecommerce.order.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItemRequest {

    @NotNull(message = "SKU ID is required")
    @JsonProperty("sku_id")
    private UUID skuId;

    @JsonProperty("product_name")
    private String productName;

    @JsonProperty("sku_code")
    private String skuCode;

    @JsonProperty("attribute_snapshot")
    private String attributeSnapshot;

    @DecimalMin(value = "0.01", message = "Unit price must be greater than 0")
    @JsonProperty("unit_price")
    private BigDecimal unitPrice;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @JsonProperty("quantity")
    private int quantity;

    public OrderItemRequest() {
    }

    public OrderItemRequest(UUID skuId, int quantity) {
        this.skuId = skuId;
        this.quantity = quantity;
    }

    public OrderItemRequest(UUID skuId, String productName, String skuCode,
                            String attributeSnapshot, BigDecimal unitPrice, int quantity) {
        this.skuId = skuId;
        this.productName = productName;
        this.skuCode = skuCode;
        this.attributeSnapshot = attributeSnapshot;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public void setSkuId(UUID skuId) {
        this.skuId = skuId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getAttributeSnapshot() {
        return attributeSnapshot;
    }

    public void setAttributeSnapshot(String attributeSnapshot) {
        this.attributeSnapshot = attributeSnapshot;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
