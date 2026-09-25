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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public RegisterCustomerResponse register(RegisterCustomerRequest request) {
        if (userAccountRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        UserAccount account = new UserAccount(
                request.getEmail(),
                hashedPassword,
                AccountType.CUSTOMER,
                request.getFullName()
        );

        UserAccount saved = userAccountRepository.save(account);

        return new RegisterCustomerResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getStatus().name()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount account = userAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationFailedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new AuthenticationFailedException("Invalid email or password");
        }

        if (account.getStatus() == AccountStatus.LOCKED) {
            throw new AuthorizationFailedException("Account is locked");
        }

        List<String> roleCodes = account.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toList());

        String token = jwtTokenProvider.generateToken(
                account.getId().toString(),
                account.getEmail(),
                roleCodes
        );

        return new AuthResponse(
                token,
                jwtTokenProvider.getExpirationMs() / 1000,
                roleCodes
        );
    }
}
