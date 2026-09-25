package com.ecommerce.identity.service;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.identity.api.dto.UserResponse;
import com.ecommerce.identity.domain.model.AccountStatus;
import com.ecommerce.identity.domain.model.Role;
import com.ecommerce.identity.domain.model.UserAccount;
import com.ecommerce.identity.domain.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserAccountRepository userAccountRepository;

    public UserService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public UserResponse lockAccount(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        user.setStatus(AccountStatus.LOCKED);
        UserAccount saved = userAccountRepository.save(user);

        return mapToResponse(saved);
    }

    @Transactional
    public UserResponse unlockAccount(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        user.setStatus(AccountStatus.ACTIVE);
        UserAccount saved = userAccountRepository.save(user);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userAccountRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
