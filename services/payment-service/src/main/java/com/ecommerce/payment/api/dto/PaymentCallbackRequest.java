package com.ecommerce.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class PaymentCallbackRequest {

    @NotNull(message = "Order ID is required")
    @JsonProperty("order_id")
    @JsonAlias({"orderId", "order_id"})
    private UUID orderId;

    @NotBlank(message = "Provider reference is required")
    @JsonProperty("provider_reference")
    @JsonAlias({"providerReference", "provider_reference"})
    private String providerReference;

    @NotBlank(message = "Result is required")
    @JsonProperty("result")
    @JsonAlias({"result"})
    private String result; // SUCCESS or FAILED

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @JsonProperty("amount")
    @JsonAlias({"amount"})
    private BigDecimal amount;

    public PaymentCallbackRequest() {
    }

    public PaymentCallbackRequest(UUID orderId, String providerReference, String result, BigDecimal amount) {
        this.orderId = orderId;
        this.providerReference = providerReference;
        this.result = result;
        this.amount = amount;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getProviderReference() {
        return providerReference;
    }

    public void setProviderReference(String providerReference) {
        this.providerReference = providerReference;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
