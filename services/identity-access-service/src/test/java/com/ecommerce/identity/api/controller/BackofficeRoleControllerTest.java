package com.ecommerce.identity.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.ConflictException;
import com.ecommerce.common.error.GlobalExceptionHandler;
import com.ecommerce.identity.api.dto.AssignPermissionRequest;
import com.ecommerce.identity.api.dto.CreateRoleRequest;
import com.ecommerce.identity.api.dto.PermissionResponse;
import com.ecommerce.identity.api.dto.RoleResponse;
import com.ecommerce.identity.service.RbacService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BackofficeRoleControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RbacService rbacService;

    @InjectMocks
    private BackofficeRoleController roleController;

    private final UUID roleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API-RBAC-001 Happy Path: POST /api/v1/backoffice/roles creates role with zero permissions (BR-018)")
    void createRole_HappyPath() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest("WAREHOUSE_STAFF");
        RoleResponse response = new RoleResponse(roleId, "WAREHOUSE_STAFF", List.of());

        when(rbacService.createRole(any(CreateRoleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/backoffice/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("WAREHOUSE_STAFF"))
                .andExpect(jsonPath("$.permissions").isEmpty());
    }

    @Test
    @DisplayName("API-RBAC-001 Conflict: POST /api/v1/backoffice/roles with existing code returns 409 CONFLICT")
    void createRole_Duplicate_Returns409() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest("SUPER_ADMIN");

        when(rbacService.createRole(any(CreateRoleRequest.class)))
                .thenThrow(new ConflictException("Role already exists: SUPER_ADMIN"));

        mockMvc.perform(post("/api/v1/backoffice/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("API-RBAC-001 Happy Path: POST assign permission to role returns 200 OK")
    void assignPermission_HappyPath() throws Exception {
        AssignPermissionRequest request = new AssignPermissionRequest("ORDER_READ");
        RoleResponse response = new RoleResponse(roleId, "OPS_ADMIN", List.of("ORDER_READ"));

        when(rbacService.assignPermission(eq(roleId), eq("ORDER_READ"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/backoffice/roles/{roleId}/permissions", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions[0]").value("ORDER_READ"));
    }

    @Test
    @DisplayName("API-RBAC-001 Business Rule: Invalid permission returns 422 BUSINESS_RULE_VIOLATION (BR-018)")
    void assignPermission_InvalidCode_Returns422() throws Exception {
        AssignPermissionRequest request = new AssignPermissionRequest("INVALID_PERM");

        when(rbacService.assignPermission(eq(roleId), eq("INVALID_PERM")))
                .thenThrow(new BusinessRuleException("BR-018", "Invalid permission code: INVALID_PERM"));

        mockMvc.perform(post("/api/v1/backoffice/roles/{roleId}/permissions", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.violated_rule").value("BR-018"));
    }

    @Test
    @DisplayName("API-RBAC-001 Happy Path: GET /api/v1/backoffice/roles returns 200 OK with role list")
    void getRoles_HappyPath() throws Exception {
        RoleResponse role = new RoleResponse(roleId, "SUPER_ADMIN", List.of("USER_ROLE_MANAGE"));
        when(rbacService.getRoles()).thenReturn(List.of(role));

        mockMvc.perform(get("/api/v1/backoffice/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("SUPER_ADMIN"));
    }

    @Test
    @DisplayName("API-RBAC-001 Happy Path: GET /api/v1/backoffice/permissions returns 200 OK with permission list")
    void getPermissions_HappyPath() throws Exception {
        PermissionResponse perm = new PermissionResponse(UUID.randomUUID(), "USER_ROLE_MANAGE");
        when(rbacService.getAllPermissions()).thenReturn(List.of(perm));

        mockMvc.perform(get("/api/v1/backoffice/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("USER_ROLE_MANAGE"));
    }
}
