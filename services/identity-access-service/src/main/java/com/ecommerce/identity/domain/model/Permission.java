package com.ecommerce.identity.domain.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "permission")
public class Permission {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    public Permission() {
        this.id = UUID.randomUUID();
    }

    public Permission(String code) {
        this.id = UUID.randomUUID();
        this.code = code;
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
}
