package com.ecommerce.identity.api.dto;

import java.util.List;
import java.util.UUID;

public class UserResponse {

    private UUID userId;
    private String email;
    private String status;
    private List<String> roles;

    public UserResponse() {
    }

    public UserResponse(UUID userId, String email, String status, List<String> roles) {
        this.userId = userId;
        this.email = email;
        this.status = status;
        this.roles = roles;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
