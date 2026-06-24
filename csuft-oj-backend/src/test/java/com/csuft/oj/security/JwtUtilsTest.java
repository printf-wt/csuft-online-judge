package com.csuft.oj.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtUtilsTest {

    @Test
    void tokenCarriesSessionVersion() {
        JwtUtils jwtUtils = new JwtUtils("test-jwt-secret-with-at-least-thirty-two-bytes", 900);

        String token = jwtUtils.generateToken(7L, "student", "STUDENT", 4);
        JwtUserPrincipal principal = jwtUtils.getPrincipal(token);

        assertEquals(7L, principal.getUserId());
        assertEquals("student", principal.getUsername());
        assertEquals("STUDENT", principal.getRole());
        assertEquals(4, principal.getTokenVersion());
    }
}
