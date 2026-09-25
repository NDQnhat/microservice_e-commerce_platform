package com.ecommerce.identity.api.dto;

import java.util.List;
import java.util.UUID;

public class RoleResponse {

    private UUID id;
    private String code;
    private List<String> permissions;

    public RoleResponse() {
    }

    public RoleResponse(UUID id, String code, List<String> permissions) {
        this.id = id;
        this.code = code;
        this.permissions = permissions;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
