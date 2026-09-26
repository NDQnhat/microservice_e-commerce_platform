package com.ecommerce.fulfillment.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public class ShipmentDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("order_id")
    private UUID orderId;

    @JsonProperty("carrier_name")
    private String carrierName;

    @JsonProperty("tracking_code")
    private String trackingCode;

    @JsonProperty("status")
    private String status;

    @JsonProperty("packed_at")
    private Instant packedAt;

    @JsonProperty("shipped_at")
    private Instant shippedAt;

    @JsonProperty("delivered_at")
    private Instant deliveredAt;

    public ShipmentDto() {
    }

    public ShipmentDto(UUID id, UUID orderId, String carrierName, String trackingCode,
                       String status, Instant packedAt, Instant shippedAt, Instant deliveredAt) {
        this.id = id;
        this.orderId = orderId;
        this.carrierName = carrierName;
        this.trackingCode = trackingCode;
        this.status = status;
        this.packedAt = packedAt;
        this.shippedAt = shippedAt;
        this.deliveredAt = deliveredAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getPackedAt() {
        return packedAt;
    }

    public void setPackedAt(Instant packedAt) {
        this.packedAt = packedAt;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(Instant shippedAt) {
        this.shippedAt = shippedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }
}
