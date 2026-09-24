package com.ecommerce.pricing.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CreatePricePromotionRequest {

    @NotNull(message = "SKU ID is required")
    private UUID skuId;

    @NotNull(message = "Sale price is required")
    @DecimalMin(value = "0.01", message = "Sale price must be greater than 0")
    private BigDecimal salePrice;

    @NotNull(message = "Start timestamp is required")
    private Instant startAt;

    @NotNull(message = "End timestamp is required")
    private Instant endAt;

    public CreatePricePromotionRequest() {
    }

    public CreatePricePromotionRequest(UUID skuId, BigDecimal salePrice, Instant startAt, Instant endAt) {
        this.skuId = skuId;
        this.salePrice = salePrice;
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public void setSkuId(UUID skuId) {
        this.skuId = skuId;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }
}
