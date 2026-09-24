package com.ecommerce.inventory.api.dto;

import com.ecommerce.inventory.domain.model.AdjustmentReasonCode;
import jakarta.validation.constraints.NotNull;

public class AdjustInventoryRequest {

    @NotNull(message = "Delta is required")
    private Integer delta;

    @NotNull(message = "Reason code is required")
    private AdjustmentReasonCode reasonCode;

    private String note;

    public AdjustInventoryRequest() {
    }

    public AdjustInventoryRequest(Integer delta, AdjustmentReasonCode reasonCode, String note) {
        this.delta = delta;
        this.reasonCode = reasonCode;
        this.note = note;
    }

    public Integer getDelta() {
        return delta;
    }

    public void setDelta(Integer delta) {
        this.delta = delta;
    }

    public AdjustmentReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(AdjustmentReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
