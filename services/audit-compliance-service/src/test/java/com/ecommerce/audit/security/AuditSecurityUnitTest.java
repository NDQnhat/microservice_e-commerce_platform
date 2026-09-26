package com.ecommerce.audit.security;

import com.ecommerce.common.error.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditSecurityUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtTokenProvider = new JwtTokenProvider("c3VwZXItc2VjcmV0LWtleS1mb3ItZGlzdHJpYnV0ZWQtZWNvbW1lcmNl", 3600000);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);
        securityConfig = new SecurityConfig(jwtAuthenticationFilter, objectMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("JwtTokenProvider: generates valid system token with roles and claims")
    void jwtTokenProvider_generatesAndValidatesToken() {
        String token = jwtTokenProvider.generateSystemToken();
        assertThat(token).isNotBlank();

        Claims claims = jwtTokenProvider.validateAndGetClaims(token);
        assertThat(claims.getSubject()).isEqualTo("00000000-0000-0000-0000-000000000000");
        assertThat(claims.get("email")).isEqualTo("audit-service@ecommerce.internal");

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        assertThat(roles).contains("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
    }

    @Test
    @DisplayName("UserPrincipal: wraps user claims and maps authorities correctly")
    void userPrincipal_mapsAuthorities() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(userId, "admin@example.com", List.of("ADMIN", "ROLE_SUPER_ADMIN"));

        assertThat(principal.getId()).isEqualTo(userId);
        assertThat(principal.getUsername()).isEqualTo("admin@example.com");
        assertThat(principal.getAuthorities()).hasSize(2);
        assertThat(principal.getAuthorities().stream().map(Object::toString).toList())
                .contains("ROLE_ADMIN", "ROLE_SUPER_ADMIN");
    }

    @Test
    @DisplayName("AuthenticationEntryPoint: outputs RFC 7807 Problem Details 401 AUTHENTICATION_FAILED")
    void authenticationEntryPoint_writesRfc7807() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/backoffice/audit-log");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException authException = mock(AuthenticationException.class);

        securityConfig.authenticationEntryPoint().commence(request, response, authException);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");

        ApiErrorResponse body = objectMapper.readValue(response.getContentAsString(), ApiErrorResponse.class);
        assertThat(body.getCode()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(body.getStatus()).isEqualTo(401);
        assertThat(body.getType()).isEqualTo("urn:problem-type:authentication-failed");
    }

    @Test
    @DisplayName("AccessDeniedHandler: outputs RFC 7807 Problem Details 403 AUTHORIZATION_FAILED")
    void accessDeniedHandler_writesRfc7807() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/backoffice/audit-log");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AccessDeniedException accessDeniedException = new AccessDeniedException("Forbidden");

        securityConfig.accessDeniedHandler().handle(request, response, accessDeniedException);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).contains("application/json");

        ApiErrorResponse body = objectMapper.readValue(response.getContentAsString(), ApiErrorResponse.class);
        assertThat(body.getCode()).isEqualTo("AUTHORIZATION_FAILED");
        assertThat(body.getStatus()).isEqualTo(403);
        assertThat(body.getType()).isEqualTo("urn:problem-type:authorization-failed");
    }

    @Test
    @DisplayName("JwtAuthenticationFilter: sets authentication in context when valid Bearer token provided")
    void jwtFilter_setsAuthenticationOnValidToken() throws Exception {
        String token = jwtTokenProvider.generateSystemToken();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("audit-service@ecommerce.internal");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("JwtAuthenticationFilter: leaves context unauthenticated when header missing or invalid")
    void jwtFilter_leavesContextUnauthenticatedWhenNoToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
