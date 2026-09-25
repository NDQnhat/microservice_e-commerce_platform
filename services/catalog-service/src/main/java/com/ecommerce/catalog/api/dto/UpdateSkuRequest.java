package com.ecommerce.catalog.api.dto;

import java.util.Set;
import java.util.UUID;

public class UpdateSkuRequest {

    private String skuCode;
    private String status;
    private Set<UUID> attributeValueIds;

    public UpdateSkuRequest() {
    }

    public UpdateSkuRequest(String skuCode, String status, Set<UUID> attributeValueIds) {
        this.skuCode = skuCode;
        this.status = status;
        this.attributeValueIds = attributeValueIds;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Set<UUID> getAttributeValueIds() {
        return attributeValueIds;
    }

    public void setAttributeValueIds(Set<UUID> attributeValueIds) {
        this.attributeValueIds = attributeValueIds;
    }
}
