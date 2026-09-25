package com.ecommerce.identity.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.identity.api.dto.AssignRoleRequest;
import com.ecommerce.identity.api.dto.UserResponse;
import com.ecommerce.identity.security.UserPrincipal;
import com.ecommerce.identity.service.RbacService;
import com.ecommerce.identity.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BackofficeUserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UserService userService;

    @Mock
    private RbacService rbacService;

    @InjectMocks
    private BackofficeUserController userController;

    private final UUID adminId = UUID.randomUUID();
    private final UUID targetUserId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        UserPrincipal admin = new UserPrincipal(adminId, "admin@example.com", List.of("SUPER_ADMIN"));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("API-IAM-005 Happy Path: POST lock user returns 200 OK with LOCKED status")
    void lockUser_HappyPath() throws Exception {
        UserResponse response = new UserResponse(targetUserId, "user@example.com", "LOCKED", List.of("CUSTOMER"));
        when(userService.lockAccount(targetUserId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/backoffice/users/{userId}/lock", targetUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));
    }

    @Test
    @DisplayName("API-IAM-005 Happy Path: POST unlock user returns 200 OK with ACTIVE status")
    void unlockUser_HappyPath() throws Exception {
        UserResponse response = new UserResponse(targetUserId, "user@example.com", "ACTIVE", List.of("CUSTOMER"));
        when(userService.unlockAccount(targetUserId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/backoffice/users/{userId}/unlock", targetUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("API-IAM-005 Not Found: POST lock non-existent user returns 404 NOT_FOUND")
    void lockUser_NotFound() throws Exception {
        when(userService.lockAccount(targetUserId))
                .thenThrow(new NotFoundException("User not found: " + targetUserId));

        mockMvc.perform(post("/api/v1/backoffice/users/{userId}/lock", targetUserId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("API-IAM-004 Happy Path: POST assign role returns 200 OK with updated roles")
    void assignRole_HappyPath() throws Exception {
        AssignRoleRequest request = new AssignRoleRequest("OPS_ADMIN");
        UserResponse response = new UserResponse(targetUserId, "user@example.com", "ACTIVE", List.of("CUSTOMER", "OPS_ADMIN"));
        when(rbacService.assignRole(eq(targetUserId), eq("OPS_ADMIN"), eq(adminId.toString()))).thenReturn(response);

        mockMvc.perform(post("/api/v1/backoffice/users/{userId}/roles", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[1]").value("OPS_ADMIN"));
    }

    @Test
    @DisplayName("API-IAM-004 Business Rule: POST assign invalid role code returns 422 BUSINESS_RULE_VIOLATION")
    void assignRole_InvalidCode_Returns422() throws Exception {
        AssignRoleRequest request = new AssignRoleRequest("INVALID_ROLE");
        when(rbacService.assignRole(eq(targetUserId), eq("INVALID_ROLE"), eq(adminId.toString())))
                .thenThrow(new BusinessRuleException("BR-018", "Invalid role code: INVALID_ROLE"));

        mockMvc.perform(post("/api/v1/backoffice/users/{userId}/roles", targetUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violated_rule").value("BR-018"));
    }

    @Test
    @DisplayName("API-IAM-004 Happy Path: DELETE revoke role returns 200 OK")
    void revokeRole_HappyPath() throws Exception {
        UserResponse response = new UserResponse(targetUserId, "user@example.com", "ACTIVE", List.of("CUSTOMER"));
        when(rbacService.revokeRole(eq(targetUserId), eq(roleId), eq(adminId.toString()))).thenReturn(response);

        mockMvc.perform(delete("/api/v1/backoffice/users/{userId}/roles/{roleId}", targetUserId, roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray());
    }
}
