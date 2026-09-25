package com.ecommerce.identity.service;

import com.ecommerce.common.error.AuthenticationFailedException;
import com.ecommerce.common.error.AuthorizationFailedException;
import com.ecommerce.common.error.ConflictException;
import com.ecommerce.identity.api.dto.AuthResponse;
import com.ecommerce.identity.api.dto.LoginRequest;
import com.ecommerce.identity.api.dto.RegisterCustomerRequest;
import com.ecommerce.identity.api.dto.RegisterCustomerResponse;
import com.ecommerce.identity.domain.model.AccountStatus;
import com.ecommerce.identity.domain.model.AccountType;
import com.ecommerce.identity.domain.model.Role;
import com.ecommerce.identity.domain.model.UserAccount;
import com.ecommerce.identity.domain.repository.UserAccountRepository;
import com.ecommerce.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userAccountRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("FR-001 Happy Path: Successful registration creates active customer account with hashed password")
    void register_HappyPath() {
        RegisterCustomerRequest request = new RegisterCustomerRequest("alice@example.com", "Secret123!", "Alice Wonder");
        when(userAccountRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123!")).thenReturn("hashed_secret");

        UserAccount savedAccount = new UserAccount("alice@example.com", "hashed_secret", AccountType.CUSTOMER, "Alice Wonder");
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(savedAccount);

        RegisterCustomerResponse response = authService.register(request);

        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(passwordEncoder).encode("Secret123!");
        verify(userAccountRepository).save(any(UserAccount.class));
    }

    @Test
    @DisplayName("FR-001 Conflict: Register with existing email throws ConflictException (409)")
    void register_DuplicateEmail_ThrowsConflict() {
        RegisterCustomerRequest request = new RegisterCustomerRequest("alice@example.com", "Secret123!", "Alice Wonder");
        when(userAccountRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Email already registered");

        verify(userAccountRepository, never()).save(any(UserAccount.class));
    }

    @Test
    @DisplayName("FR-002 Happy Path: Login with valid credentials issues JWT token")
    void login_HappyPath() {
        LoginRequest request = new LoginRequest("bob@example.com", "Secret123!");
        UserAccount account = new UserAccount("bob@example.com", "hashed_secret", AccountType.CUSTOMER, "Bob Smith");
        account.getRoles().add(new Role("CUSTOMER"));

        when(userAccountRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Secret123!", "hashed_secret")).thenReturn(true);
        when(jwtTokenProvider.generateToken(anyString(), eq("bob@example.com"), any())).thenReturn("jwt_token_123");
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("jwt_token_123");
        assertThat(response.getExpiresIn()).isEqualTo(86400);
        assertThat(response.getRoles()).contains("CUSTOMER");
    }

    @Test
    @DisplayName("FR-002 Failure Path: Login with wrong password throws AuthenticationFailedException (401)")
    void login_WrongPassword_ThrowsAuthenticationFailed() {
        LoginRequest request = new LoginRequest("bob@example.com", "WrongPassword");
        UserAccount account = new UserAccount("bob@example.com", "hashed_secret", AccountType.CUSTOMER, "Bob Smith");

        when(userAccountRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("WrongPassword", "hashed_secret")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("FR-002 Failure Path: Login with non-existent email throws AuthenticationFailedException (401)")
    void login_UnknownEmail_ThrowsAuthenticationFailed() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password");
        when(userAccountRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    @DisplayName("FR-002 Business Rule: Locked account is forbidden from logging in (403 AUTHORIZATION_FAILED)")
    void login_LockedAccount_ThrowsAuthorizationFailed() {
        LoginRequest request = new LoginRequest("locked@example.com", "Secret123!");
        UserAccount account = new UserAccount("locked@example.com", "hashed_secret", AccountType.CUSTOMER, "Locked User");
        account.setStatus(AccountStatus.LOCKED);

        when(userAccountRepository.findByEmail("locked@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Secret123!", "hashed_secret")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthorizationFailedException.class)
                .hasMessage("Account is locked");

        verify(jwtTokenProvider, never()).generateToken(anyString(), anyString(), any());
    }
}
