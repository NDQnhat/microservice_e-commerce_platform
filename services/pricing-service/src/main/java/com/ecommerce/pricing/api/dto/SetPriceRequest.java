package com.ecommerce.pricing.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class SetPriceRequest {

    @NotNull(message = "SKU ID is required")
    @JsonProperty("sku_id")
    private UUID skuId;

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.01", message = "Base price must be greater than 0")
    @JsonProperty("base_price")
    private BigDecimal basePrice;

    @NotBlank(message = "Currency is required")
    @JsonProperty("currency")
    private String currency;

    public SetPriceRequest() {
    }

    public SetPriceRequest(UUID skuId, BigDecimal basePrice, String currency) {
        this.skuId = skuId;
        this.basePrice = basePrice;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
