package com.ecommerce.identity.api.dto;

import java.util.List;

public class AuthResponse {

    private String accessToken;
    private long expiresIn;
    private List<String> roles;

    public AuthResponse() {
    }

    public AuthResponse(String accessToken, long expiresIn, List<String> roles) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.roles = roles;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
