package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login response containing JWT and user profile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthLoginVO {

    /**
     * JWT access token.
     */
    private String token;

    /**
     * Token type used by the Authorization header.
     */
    private String tokenType;

    /**
     * Token lifetime in seconds.
     */
    private Long expiresIn;

    /**
     * Logged-in user profile.
     */
    private AuthUserVO user;
}
