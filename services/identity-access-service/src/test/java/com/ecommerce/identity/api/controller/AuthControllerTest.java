package com.ecommerce.identity.api.controller;

import com.ecommerce.common.error.AuthenticationFailedException;
import com.ecommerce.common.error.AuthorizationFailedException;
import com.ecommerce.common.error.ConflictException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.identity.api.dto.AuthResponse;
import com.ecommerce.identity.api.dto.LoginRequest;
import com.ecommerce.identity.api.dto.RegisterCustomerRequest;
import com.ecommerce.identity.api.dto.RegisterCustomerResponse;
import com.ecommerce.identity.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API-IAM-001 Happy Path: POST /api/v1/customers/register returns 201 Created")
    void register_HappyPath() throws Exception {
        RegisterCustomerRequest request = new RegisterCustomerRequest("alice@example.com", "Password123!", "Alice Wonder");
        RegisterCustomerResponse response = new RegisterCustomerResponse(UUID.randomUUID(), "alice@example.com", "ACTIVE");

        when(authService.register(any(RegisterCustomerRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("API-IAM-001 Validation Error: Missing email returns 400 VALIDATION_ERROR")
    void register_ValidationError_MissingEmail() throws Exception {
        RegisterCustomerRequest request = new RegisterCustomerRequest("", "Password123!", "Alice Wonder");

        mockMvc.perform(post("/api/v1/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("API-IAM-001 Conflict Error: Duplicate email returns 409 CONFLICT")
    void register_ConflictError_DuplicateEmail() throws Exception {
        RegisterCustomerRequest request = new RegisterCustomerRequest("existing@example.com", "Password123!", "Alice Wonder");

        when(authService.register(any(RegisterCustomerRequest.class)))
                .thenThrow(new ConflictException("Email already registered: existing@example.com"));

        mockMvc.perform(post("/api/v1/customers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Email already registered: existing@example.com"));
    }

    @Test
    @DisplayName("API-IAM-002 Happy Path: POST /api/v1/auth/login returns 200 OK with bearer token")
    void login_HappyPath() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "Password123!");
        AuthResponse response = new AuthResponse("jwt-token-xyz", 86400, List.of("CUSTOMER"));

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("jwt-token-xyz"))
                .andExpect(jsonPath("$.expires_in").value(86400))
                .andExpect(jsonPath("$.roles[0]").value("CUSTOMER"));
    }

    @Test
    @DisplayName("API-IAM-002 Authentication Failure: Invalid credentials returns 401 AUTHENTICATION_FAILED")
    void login_InvalidCredentials_Returns401() throws Exception {
        LoginRequest request = new LoginRequest("alice@example.com", "WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationFailedException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    @DisplayName("API-IAM-002 Authorization Failure: Locked account returns 403 AUTHORIZATION_FAILED")
    void login_LockedAccount_Returns403() throws Exception {
        LoginRequest request = new LoginRequest("locked@example.com", "Password123!");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthorizationFailedException("Account is locked"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTHORIZATION_FAILED"))
                .andExpect(jsonPath("$.detail").value("Account is locked"));
    }
}
