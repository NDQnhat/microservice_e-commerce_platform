package com.ecommerce.identity.api.controller;

import com.ecommerce.identity.api.dto.AssignPermissionRequest;
import com.ecommerce.identity.api.dto.CreateRoleRequest;
import com.ecommerce.identity.api.dto.PermissionResponse;
import com.ecommerce.identity.api.dto.RoleResponse;
import com.ecommerce.identity.service.RbacService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice")
public class BackofficeRoleController {

    private final RbacService rbacService;

    public BackofficeRoleController(RbacService rbacService) {
        this.rbacService = rbacService;
    }

    @PostMapping("/roles")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleResponse response = rbacService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/roles/{roleId}/permissions")
    public ResponseEntity<RoleResponse> assignPermission(@PathVariable UUID roleId,
                                                         @Valid @RequestBody AssignPermissionRequest request) {
        RoleResponse response = rbacService.assignPermission(roleId, request.getPermissionCode());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getRoles() {
        List<RoleResponse> responses = rbacService.getRoles();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/roles/{roleId}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable UUID roleId) {
        RoleResponse response = rbacService.getRoleById(roleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/permissions")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        List<PermissionResponse> responses = rbacService.getAllPermissions();
        return ResponseEntity.ok(responses);
    }
}
