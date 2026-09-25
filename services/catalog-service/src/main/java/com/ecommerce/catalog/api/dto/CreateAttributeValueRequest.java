package com.ecommerce.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateAttributeValueRequest {

    @NotBlank(message = "Attribute value is required")
    private String value;

    public CreateAttributeValueRequest() {
    }

    public CreateAttributeValueRequest(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
