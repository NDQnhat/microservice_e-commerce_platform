package com.ecommerce.order.security;

import com.ecommerce.common.error.AuthenticationFailedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        throw new AuthenticationFailedException("Unauthenticated request");
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            String roleName = role.startsWith("ROLE_") ? role.substring(5) : role;
            return principal.getRoles().contains(roleName) || principal.getRoles().contains("ROLE_" + roleName);
        }
        return false;
    }

    public static boolean isBackofficeAdmin() {
        return hasRole("ORDER_OPERATIONS_ADMIN") || hasRole("ADMIN") || hasRole("SUPER_ADMIN") || hasRole("OPS_ADMIN");
    }
}
