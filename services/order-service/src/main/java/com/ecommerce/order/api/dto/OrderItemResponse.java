package com.ecommerce.order.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItemResponse {

    private UUID id;
    private UUID skuId;
    private String productNameSnapshot;
    private String skuCodeSnapshot;
    private String attributeSnapshot;
    private BigDecimal unitPriceSnapshot;
    private int quantity;
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
