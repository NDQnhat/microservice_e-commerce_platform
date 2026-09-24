package com.ecommerce.payment.api.dto;

import jakarta.validation.constraints.NotBlank;

public class ManualReconcileRequest {

    @NotBlank(message = "Evidence reference is mandatory for reconciliation per BR-012")
    private String evidenceReference;

    @NotBlank(message = "Reason is mandatory for reconciliation per BR-012")
    private String reason;

    public ManualReconcileRequest() {
    }

    public ManualReconcileRequest(String evidenceReference, String reason) {
        this.evidenceReference = evidenceReference;
        this.reason = reason;
    }

    public String getEvidenceReference() {
        return evidenceReference;
    }

    public void setEvidenceReference(String evidenceReference) {
        this.evidenceReference = evidenceReference;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
