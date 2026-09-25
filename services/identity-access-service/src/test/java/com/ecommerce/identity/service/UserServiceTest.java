package com.ecommerce.identity.service;

import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.identity.api.dto.UserResponse;
import com.ecommerce.identity.domain.model.AccountStatus;
import com.ecommerce.identity.domain.model.AccountType;
import com.ecommerce.identity.domain.model.Role;
import com.ecommerce.identity.domain.model.UserAccount;
import com.ecommerce.identity.domain.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private UserService userService;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userService = new UserService(userAccountRepository);
    }

    @Test
    @DisplayName("FR-018 Happy Path: Lock account mutates status to LOCKED (BR-005)")
    void lockAccount_HappyPath() {
        UserAccount user = new UserAccount("user@example.com", "hash", AccountType.CUSTOMER, "John");
        user.setStatus(AccountStatus.ACTIVE);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(user)).thenReturn(user);

        UserResponse response = userService.lockAccount(userId);

        assertThat(response.getStatus()).isEqualTo("LOCKED");
        assertThat(user.getStatus()).isEqualTo(AccountStatus.LOCKED);
        verify(userAccountRepository).save(user);
    }

    @Test
    @DisplayName("FR-018 Happy Path: Unlock account mutates status to ACTIVE (BR-005)")
    void unlockAccount_HappyPath() {
        UserAccount user = new UserAccount("user@example.com", "hash", AccountType.CUSTOMER, "John");
        user.setStatus(AccountStatus.LOCKED);
        when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userAccountRepository.save(user)).thenReturn(user);

        UserResponse response = userService.unlockAccount(userId);

        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(userAccountRepository).save(user);
    }

    @Test
    @DisplayName("FR-018 Failure Path: Lock non-existent user throws NotFoundException (404)")
    void lockAccount_NotFound() {
        when(userAccountRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.lockAccount(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
