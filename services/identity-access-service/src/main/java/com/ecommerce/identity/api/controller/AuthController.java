package com.ecommerce.identity.api.controller;

import com.ecommerce.identity.api.dto.AuthResponse;
import com.ecommerce.identity.api.dto.LoginRequest;
import com.ecommerce.identity.api.dto.RegisterCustomerRequest;
import com.ecommerce.identity.api.dto.RegisterCustomerResponse;
import com.ecommerce.identity.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/customers/register")
    public ResponseEntity<RegisterCustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
        RegisterCustomerResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
