package com.csuft.oj.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

/**
 * Helpers for reading the current Spring Security principal.
 */
public final class SecurityUtils {

    private static final Set<String> PRIVILEGED_ROLES = Set.of("TEACHER", "ADMIN");

    private SecurityUtils() {
    }

    /**
     * Returns the current JWT principal if the request is authenticated.
     */
    public static Optional<JwtUserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    /**
     * Returns the current user ID when available.
     */
    public static Long currentUserIdOrNull() {
        return currentPrincipal().map(JwtUserPrincipal::getUserId).orElse(null);
    }

    /**
     * Returns true for teachers and administrators.
     */
    public static boolean canViewHiddenProblems() {
        return isTeacherOrAdmin();
    }

    /**
     * Returns true for teachers and administrators.
     */
    public static boolean isTeacherOrAdmin() {
        return currentPrincipal()
                .map(JwtUserPrincipal::getRole)
                .map(PRIVILEGED_ROLES::contains)
                .orElse(false);
    }

    /**
     * Returns true for administrators.
     */
    public static boolean isAdmin() {
        return currentPrincipal()
                .map(JwtUserPrincipal::getRole)
                .map("ADMIN"::equals)
                .orElse(false);
    }
}
