package com.ecommerce.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateRoleRequest {

    @NotBlank(message = "Role code is required")
    @Pattern(regexp = "^[A-Z0-9_]{3,64}$", message = "Role code must be uppercase alphanumeric with underscores (3-64 characters)")
    private String code;

    public CreateRoleRequest() {
    }

    public CreateRoleRequest(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
