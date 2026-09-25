package com.ecommerce.catalog.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateAttributeRequest {

    @NotBlank(message = "Attribute name is required")
    private String name;

    public CreateAttributeRequest() {
    }

    public CreateAttributeRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
