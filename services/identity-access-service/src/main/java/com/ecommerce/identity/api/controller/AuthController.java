package com.ecommerce.identity.api.controller;

import com.ecommerce.common.error.ConflictException;
import com.ecommerce.common.error.NotFoundException;
import com.ecommerce.identity.api.dto.AuthResponse;
import com.ecommerce.identity.api.dto.LoginRequest;
import com.ecommerce.identity.api.dto.RegisterCustomerRequest;
import com.ecommerce.identity.api.dto.RegisterCustomerResponse;
import com.ecommerce.identity.domain.model.AccountStatus;
import com.ecommerce.identity.domain.model.AccountType;
import com.ecommerce.identity.domain.model.Role;
import com.ecommerce.identity.domain.model.UserAccount;
import com.ecommerce.identity.domain.repository.RoleRepository;
import com.ecommerce.identity.domain.repository.UserAccountRepository;
import com.ecommerce.identity.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(UserAccountRepository userAccountRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          JwtTokenProvider jwtTokenProvider) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/customers/register")
    public ResponseEntity<RegisterCustomerResponse> register(
            @Valid @RequestBody RegisterCustomerRequest request) {
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

        RegisterCustomerResponse response = new RegisterCustomerResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getStatus().name()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        UserAccount account = userAccountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new NotFoundException("Invalid credentials");
        }

        if (account.getStatus() == AccountStatus.LOCKED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<String> roleCodes = account.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toList());

        String token = jwtTokenProvider.generateToken(
                account.getId().toString(),
                account.getEmail(),
                roleCodes
        );

        AuthResponse response = new AuthResponse(
                token,
                jwtTokenProvider.getExpirationMs() / 1000,
                roleCodes
        );

        return ResponseEntity.ok(response);
    }
}
