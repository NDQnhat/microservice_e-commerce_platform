package com.ecommerce.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignPermissionRequest {

    @NotBlank(message = "Permission code is required")
    private String permissionCode;

    public AssignPermissionRequest() {
    }

    public AssignPermissionRequest(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }
}
