package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authenticated user profile returned to clients.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserVO {

    /**
     * User ID.
     */
    private Long id;

    /**
     * Username.
     */
    private String username;

    /**
     * Nickname.
     */
    private String nickname;

    /**
     * Email address.
     */
    private String email;

    /**
     * Role: STUDENT, TEACHER, or ADMIN.
     */
    private String role;
}
