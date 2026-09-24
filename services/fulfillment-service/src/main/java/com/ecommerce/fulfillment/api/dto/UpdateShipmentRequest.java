package com.ecommerce.fulfillment.api.dto;

import com.ecommerce.fulfillment.domain.model.ShipmentStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateShipmentRequest {

    private String carrierName;
    private String trackingCode;

    @NotNull(message = "Target status is required")
    private ShipmentStatus targetStatus;

    public UpdateShipmentRequest() {
    }

    public UpdateShipmentRequest(String carrierName, String trackingCode, ShipmentStatus targetStatus) {
        this.carrierName = carrierName;
        this.trackingCode = trackingCode;
        this.targetStatus = targetStatus;
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

    public ShipmentStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(ShipmentStatus targetStatus) {
        this.targetStatus = targetStatus;
    }
}
