package com.ecommerce.order.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

public class OrderItemResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("sku_id")
    private UUID skuId;

    @JsonProperty("product_name")
    private String productNameSnapshot;

    @JsonProperty("sku_code")
    private String skuCodeSnapshot;

    @JsonProperty("attribute_snapshot")
    private String attributeSnapshot;

    @JsonProperty("unit_price")
    private BigDecimal unitPriceSnapshot;

    @JsonProperty("quantity")
    private int quantity;

    @JsonProperty("line_total")
    private BigDecimal lineTotal;

    public OrderItemResponse() {
    }

    public OrderItemResponse(UUID id, UUID skuId, String productNameSnapshot,
                             String skuCodeSnapshot, String attributeSnapshot,
                             BigDecimal unitPriceSnapshot, int quantity, BigDecimal lineTotal) {
        this.id = id;
        this.skuId = skuId;
        this.productNameSnapshot = productNameSnapshot;
        this.skuCodeSnapshot = skuCodeSnapshot;
        this.attributeSnapshot = attributeSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public void setSkuId(UUID skuId) {
        this.skuId = skuId;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public void setProductNameSnapshot(String productNameSnapshot) {
        this.productNameSnapshot = productNameSnapshot;
    }

    public String getSkuCodeSnapshot() {
        return skuCodeSnapshot;
    }

    public void setSkuCodeSnapshot(String skuCodeSnapshot) {
        this.skuCodeSnapshot = skuCodeSnapshot;
    }

    public String getAttributeSnapshot() {
        return attributeSnapshot;
    }

    public void setAttributeSnapshot(String attributeSnapshot) {
        this.attributeSnapshot = attributeSnapshot;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) {
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
