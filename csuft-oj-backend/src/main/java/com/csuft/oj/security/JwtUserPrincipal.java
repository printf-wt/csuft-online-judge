package com.csuft.oj.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Principal restored from a verified JWT.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtUserPrincipal {

    /**
     * User ID.
     */
    private Long userId;

    /**
     * Username.
     */
    private String username;

    /**
     * Role: STUDENT, TEACHER, or ADMIN.
     */
    private String role;

    /**
     * Session version captured when the access token was issued.
     */
    private Integer tokenVersion;
}
