package com.ecommerce.identity.api.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignRoleRequest {

    @NotBlank(message = "Role code is required")
    private String roleCode;

    public AssignRoleRequest() {
    }

    public AssignRoleRequest(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
}
