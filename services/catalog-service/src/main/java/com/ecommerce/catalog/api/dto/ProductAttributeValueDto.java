package com.ecommerce.catalog.api.dto;

import java.util.UUID;

public class ProductAttributeValueDto {

    private UUID id;
    private UUID attributeId;
    private String value;

    public ProductAttributeValueDto() {
    }

    public ProductAttributeValueDto(UUID id, UUID attributeId, String value) {
        this.id = id;
        this.attributeId = attributeId;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAttributeId() {
        return attributeId;
    }

    public void setAttributeId(UUID attributeId) {
        this.attributeId = attributeId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
