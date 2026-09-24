package com.ecommerce.identity.api.dto;

import java.util.UUID;

public class RegisterCustomerResponse {

    private UUID id;
    private String email;
    private String status;

    public RegisterCustomerResponse() {
    }

    public RegisterCustomerResponse(UUID id, String email, String status) {
        this.id = id;
        this.email = email;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
}
