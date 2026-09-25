package com.ecommerce.identity.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class AuthResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
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
