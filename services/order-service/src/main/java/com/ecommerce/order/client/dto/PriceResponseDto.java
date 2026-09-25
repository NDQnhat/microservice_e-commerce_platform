package com.ecommerce.order.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

public class PriceResponseDto {

    @JsonProperty("sku_id")
    private UUID skuId;

    @JsonProperty("base_price")
    private BigDecimal basePrice;

    @JsonProperty("promotional_price")
    private BigDecimal promotionalPrice;

    @JsonProperty("effective_price")
    private BigDecimal effectivePrice;

    @JsonProperty("currency")
    private String currency;

    public PriceResponseDto() {
    }

    public PriceResponseDto(UUID skuId, BigDecimal basePrice, BigDecimal promotionalPrice,
                            BigDecimal effectivePrice, String currency) {
        this.skuId = skuId;
        this.basePrice = basePrice;
        this.promotionalPrice = promotionalPrice;
        this.effectivePrice = effectivePrice;
        this.currency = currency;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public void setSkuId(UUID skuId) {
        this.skuId = skuId;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getPromotionalPrice() {
        return promotionalPrice;
    }

    public void setPromotionalPrice(BigDecimal promotionalPrice) {
        this.promotionalPrice = promotionalPrice;
    }

    public BigDecimal getEffectivePrice() {
        return effectivePrice;
    }

    public void setEffectivePrice(BigDecimal effectivePrice) {
        this.effectivePrice = effectivePrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
