package com.ecommerce.identity.api.controller;

import com.ecommerce.identity.api.dto.AssignRoleRequest;
import com.ecommerce.identity.api.dto.UserResponse;
import com.ecommerce.identity.security.SecurityUtils;
import com.ecommerce.identity.service.RbacService;
import com.ecommerce.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/backoffice/users")
public class BackofficeUserController {

    private final UserService userService;
    private final RbacService rbacService;

    public BackofficeUserController(UserService userService, RbacService rbacService) {
        this.userService = userService;
        this.rbacService = rbacService;
    }

    @PostMapping("/{userId}/roles")
    public ResponseEntity<UserResponse> assignRole(@PathVariable UUID userId,
                                                   @Valid @RequestBody AssignRoleRequest request) {
        String actorId = SecurityUtils.getCurrentUserIdOrNull();
        UserResponse response = rbacService.assignRole(userId, request.getRoleCode(), actorId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<UserResponse> revokeRole(@PathVariable UUID userId,
                                                   @PathVariable UUID roleId) {
        String actorId = SecurityUtils.getCurrentUserIdOrNull();
        UserResponse response = rbacService.revokeRole(userId, roleId, actorId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/lock")
    public ResponseEntity<UserResponse> lockAccount(@PathVariable UUID userId) {
        UserResponse response = userService.lockAccount(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/unlock")
    public ResponseEntity<UserResponse> unlockAccount(@PathVariable UUID userId) {
        UserResponse response = userService.unlockAccount(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> responses = userService.getAllUsers();
        return ResponseEntity.ok(responses);
    }
}
