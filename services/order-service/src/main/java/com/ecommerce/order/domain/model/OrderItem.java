package com.ecommerce.order.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @Column(name = "sku_id", updatable = false)
    private UUID skuId;

    @Column(name = "product_name_snapshot", nullable = false, updatable = false)
    private String productNameSnapshot;

    @Column(name = "sku_code_snapshot", nullable = false, updatable = false)
    private String skuCodeSnapshot;

    @Column(name = "attribute_snapshot", nullable = false, columnDefinition = "text", updatable = false)
    private String attributeSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal unitPriceSnapshot;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal lineTotal;

    public OrderItem() {
        this.id = UUID.randomUUID();
    }

    public OrderItem(Order order, UUID skuId, String productNameSnapshot,
                     String skuCodeSnapshot, String attributeSnapshot,
                     BigDecimal unitPriceSnapshot, int quantity) {
        this.id = UUID.randomUUID();
        this.order = order;
        this.skuId = skuId;
        this.productNameSnapshot = productNameSnapshot;
        this.skuCodeSnapshot = skuCodeSnapshot;
        this.attributeSnapshot = attributeSnapshot;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.quantity = quantity;
        this.lineTotal = unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }

    public UUID getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public String getSkuCodeSnapshot() {
        return skuCodeSnapshot;
    }

    public String getAttributeSnapshot() {
        return attributeSnapshot;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
