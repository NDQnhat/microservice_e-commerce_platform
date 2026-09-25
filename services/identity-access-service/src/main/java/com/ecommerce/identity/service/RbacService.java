package com.ecommerce.identity.service;

import com.ecommerce.common.context.CorrelationContext;
import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.ConflictException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.identity.api.dto.*;
import com.ecommerce.identity.domain.model.*;
import com.ecommerce.identity.domain.repository.OutboxEventRepository;
import com.ecommerce.identity.domain.repository.PermissionRepository;
import com.ecommerce.identity.domain.repository.RoleRepository;
import com.ecommerce.identity.domain.repository.UserAccountRepository;
import com.ecommerce.identity.event.RoleAssignedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RbacService {

    private static final Logger log = LoggerFactory.getLogger(RbacService.class);

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public RbacService(UserAccountRepository userAccountRepository,
                       RoleRepository roleRepository,
                       PermissionRepository permissionRepository,
                       OutboxEventRepository outboxEventRepository,
                       ObjectMapper objectMapper) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UserResponse assignRole(UUID userId, String roleCode, String actorId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BusinessRuleException("BR-018", "Invalid role code: " + roleCode));

        user.getRoles().add(role);
        UserAccount saved = userAccountRepository.save(user);

        // Record RoleAssigned event in Transactional Outbox (FR-033, BR-018)
        publishRoleEvent(actorId, userId, role.getCode(), "ASSIGN");

        return mapToUserResponse(saved);
    }

    @Transactional
    public UserResponse revokeRole(UUID userId, UUID roleId, String actorId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));

        user.getRoles().remove(role);
        UserAccount saved = userAccountRepository.save(user);

        // Record RoleAssigned event in Transactional Outbox (FR-033, BR-018)
        publishRoleEvent(actorId, userId, role.getCode(), "REVOKE");

        return mapToUserResponse(saved);
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.findByCode(request.getCode()).isPresent()) {
            throw new ConflictException("Role already exists: " + request.getCode());
        }

        // BR-018: A newly created role starts with zero permissions (deny-by-default)
        Role role = new Role(request.getCode());
        Role saved = roleRepository.save(role);

        log.info("Created new role {} with zero permissions (deny-by-default)", saved.getCode());
        return mapToRoleResponse(saved);
    }

    @Transactional
    public RoleResponse assignPermission(UUID roleId, String permissionCode) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));

        Permission permission = permissionRepository.findByCode(permissionCode)
                .orElseThrow(() -> new BusinessRuleException("BR-018", "Invalid permission code: " + permissionCode));

        role.getPermissions().add(permission);
        Role saved = roleRepository.save(role);

        log.info("Assigned permission {} to role {}", permissionCode, role.getCode());
        return mapToRoleResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToRoleResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));
        return mapToRoleResponse(role);
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionResponse(p.getId(), p.getCode()))
                .collect(Collectors.toList());
    }

    private void publishRoleEvent(String actorId, UUID userId, String roleCode, String action) {
        try {
            RoleAssignedEvent event = new RoleAssignedEvent(actorId, userId, roleCode, action);
            String payload = objectMapper.writeValueAsString(event);
            OutboxEventRecord outbox = new OutboxEventRecord(
                    "USER",
                    userId.toString(),
                    "RoleAssigned",
                    payload,
                    CorrelationContext.getCorrelationId()
            );
            outboxEventRepository.save(outbox);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize RoleAssigned event", e);
            throw new RuntimeException("Outbox serialization failure", e);
        }
    }

    private UserResponse mapToUserResponse(UserAccount user) {
        List<String> roleCodes = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toList());

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus().name(),
                roleCodes
        );
    }

    private RoleResponse mapToRoleResponse(Role role) {
        List<String> permissionCodes = role.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toList());

        return new RoleResponse(
                role.getId(),
                role.getCode(),
                permissionCodes
        );
    }
}
