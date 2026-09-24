package com.ecommerce.fulfillment.api.dto;

import java.time.Instant;
import java.util.UUID;

public class ShipmentDto {

    private UUID id;
    private UUID orderId;
    private String carrierName;
    private String trackingCode;
    private String status;
    private Instant packedAt;
    private Instant shippedAt;
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
