package com.ecommerce.identity.service;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.ConflictException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.identity.api.dto.CreateRoleRequest;
import com.ecommerce.identity.api.dto.RoleResponse;
import com.ecommerce.identity.api.dto.UserResponse;
import com.ecommerce.identity.domain.model.*;
import com.ecommerce.identity.domain.repository.OutboxEventRepository;
import com.ecommerce.identity.domain.repository.PermissionRepository;
import com.ecommerce.identity.domain.repository.RoleRepository;
import com.ecommerce.identity.domain.repository.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RbacServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    private RbacService rbacService;

    private final UUID userId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        rbacService = new RbacService(
                userAccountRepository,
                roleRepository,
                permissionRepository,
                outboxEventRepository,
                objectMapper
        );
    }

    @Test
    @DisplayName("FR-033 & BR-018 Deny-by-default: Newly created role starts with zero permissions")
    void createRole_DenyByDefault() {
        CreateRoleRequest request = new CreateRoleRequest("WAREHOUSE_STAFF");
        when(roleRepository.findByCode("WAREHOUSE_STAFF")).thenReturn(Optional.empty());

        Role savedRole = new Role("WAREHOUSE_STAFF");
        when(roleRepository.save(any(Role.class))).thenReturn(savedRole);

        RoleResponse response = rbacService.createRole(request);

        assertThat(response.getCode()).isEqualTo("WAREHOUSE_STAFF");
        assertThat(response.getPermissions()).isEmpty();
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("FR-033 Conflict: Creating duplicate role code throws ConflictException (409)")
    void createRole_DuplicateCode_ThrowsConflict() {
        CreateRoleRequest request = new CreateRoleRequest("SUPER_ADMIN");
        when(roleRepository.findByCode("SUPER_ADMIN")).thenReturn(Optional.of(new Role("SUPER_ADMIN")));

        assertThatThrownBy(() -> rbacService.createRole(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Role already exists");
    }

    @Test
    @DisplayName("FR-033 Happy Path: Assign permission to role")
    void assignPermission_HappyPath() {
        Role role = new Role("OPS_ADMIN");
        Permission permission = new Permission("ORDER_READ");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findByCode("ORDER_READ")).thenReturn(Optional.of(permission));
        when(roleRepository.save(role)).thenReturn(role);

        RoleResponse response = rbacService.assignPermission(roleId, "ORDER_READ");

        assertThat(response.getPermissions()).contains("ORDER_READ");
        verify(roleRepository).save(role);
    }

    @Test
    @DisplayName("FR-033 Business Rule: Assign invalid permission code throws BusinessRuleException BR-018 (422)")
    void assignPermission_InvalidCode_ThrowsBusinessRuleException() {
        Role role = new Role("OPS_ADMIN");
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(permissionRepository.findByCode("INVALID_PERM")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rbacService.assignPermission(roleId, "INVALID_PERM"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid permission code")
                .matches(e -> "BR-018".equals(((BusinessRuleException) e).getRuleId()));
    }

    @Test
    @DisplayName("FR-033 Happy Path: Assign role records RoleAssigned outbox event with ASSIGN action")
    void assignRole_GeneratesOutboxEvent() {
        UserAccount user = new UserAccount("admin@example.com", "hash", AccountType.BACK_OFFICE, "Admin User");
        Role role = new Role("SUPER_ADMIN");

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("SUPER_ADMIN")).thenReturn(Optional.of(role));
        when(userAccountRepository.save(user)).thenReturn(user);

        UserResponse response = rbacService.assignRole(userId, "SUPER_ADMIN", "super_admin_actor");

        assertThat(response.getRoles()).contains("SUPER_ADMIN");

        ArgumentCaptor<OutboxEventRecord> outboxCaptor = ArgumentCaptor.forClass(OutboxEventRecord.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        OutboxEventRecord recordedEvent = outboxCaptor.getValue();
        assertThat(recordedEvent.getEventType()).isEqualTo("RoleAssigned");
        assertThat(recordedEvent.getAggregateType()).isEqualTo("USER");
        assertThat(recordedEvent.getAggregateId()).isEqualTo(userId.toString());
        assertThat(recordedEvent.getPayload()).contains("\"action\":\"ASSIGN\"");
        assertThat(recordedEvent.getPayload()).contains("\"roleCode\":\"SUPER_ADMIN\"");
        assertThat(recordedEvent.getPayload()).contains("\"actorId\":\"super_admin_actor\"");
    }

    @Test
    @DisplayName("FR-033 Happy Path: Revoke role records RoleAssigned outbox event with REVOKE action")
    void revokeRole_GeneratesOutboxEvent() {
        UserAccount user = new UserAccount("admin@example.com", "hash", AccountType.BACK_OFFICE, "Admin User");
        Role role = new Role("CATALOG_ADMIN");
        user.getRoles().add(role);

        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(role));
        when(userAccountRepository.save(user)).thenReturn(user);

        UserResponse response = rbacService.revokeRole(userId, roleId, "super_admin_actor");

        assertThat(response.getRoles()).doesNotContain("CATALOG_ADMIN");

        ArgumentCaptor<OutboxEventRecord> outboxCaptor = ArgumentCaptor.forClass(OutboxEventRecord.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        OutboxEventRecord recordedEvent = outboxCaptor.getValue();
        assertThat(recordedEvent.getEventType()).isEqualTo("RoleAssigned");
        assertThat(recordedEvent.getPayload()).contains("\"action\":\"REVOKE\"");
    }

    @Test
    @DisplayName("FR-033 Business Rule: Assign invalid role code throws BusinessRuleException BR-018 (422)")
    void assignRole_InvalidCode_ThrowsBusinessRuleException() {
        UserAccount user = new UserAccount("admin@example.com", "hash", AccountType.BACK_OFFICE, "Admin User");
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("NON_EXISTENT_ROLE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rbacService.assignRole(userId, "NON_EXISTENT_ROLE", "actor"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid role code")
                .matches(e -> "BR-018".equals(((BusinessRuleException) e).getRuleId()));
    }
}
