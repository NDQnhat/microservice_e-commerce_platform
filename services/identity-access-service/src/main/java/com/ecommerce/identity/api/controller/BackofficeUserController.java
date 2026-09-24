package com.ecommerce.identity.api.controller;

import com.ecommerce.common.error.BusinessRuleException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.identity.api.dto.AssignRoleRequest;
import com.ecommerce.identity.api.dto.UserResponse;
import com.ecommerce.identity.domain.model.AccountStatus;
import com.ecommerce.identity.domain.model.Role;
import com.ecommerce.identity.domain.model.UserAccount;
import com.ecommerce.identity.domain.repository.RoleRepository;
import com.ecommerce.identity.domain.repository.UserAccountRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/backoffice/users")
public class BackofficeUserController {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;

    public BackofficeUserController(UserAccountRepository userAccountRepository,
                                    RoleRepository roleRepository) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
    }

    @PostMapping("/{userId}/roles")
    @Transactional
    public ResponseEntity<UserResponse> assignRole(@PathVariable UUID userId,
                                                   @Valid @RequestBody AssignRoleRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        Role role = roleRepository.findByCode(request.getRoleCode())
                .orElseThrow(() -> new BusinessRuleException("BR-018", "Invalid role code: " + request.getRoleCode()));

        user.getRoles().add(role);
        UserAccount saved = userAccountRepository.save(user);

        return ResponseEntity.ok(mapToResponse(saved));
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @Transactional
    public ResponseEntity<UserResponse> revokeRole(@PathVariable UUID userId,
                                                   @PathVariable UUID roleId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleId));

        user.getRoles().remove(role);
        UserAccount saved = userAccountRepository.save(user);

        return ResponseEntity.ok(mapToResponse(saved));
    }

    @PostMapping("/{userId}/lock")
    @Transactional
    public ResponseEntity<UserResponse> lockAccount(@PathVariable UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        user.setStatus(AccountStatus.LOCKED);
        UserAccount saved = userAccountRepository.save(user);

        return ResponseEntity.ok(mapToResponse(saved));
    }

    @PostMapping("/{userId}/unlock")
    @Transactional
    public ResponseEntity<UserResponse> unlockAccount(@PathVariable UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        user.setStatus(AccountStatus.ACTIVE);
        UserAccount saved = userAccountRepository.save(user);

        return ResponseEntity.ok(mapToResponse(saved));
    }

    private UserResponse mapToResponse(UserAccount user) {
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
}
