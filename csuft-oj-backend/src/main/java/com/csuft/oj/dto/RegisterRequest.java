package com.csuft.oj.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registration request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    /**
     * Username.
     */
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 32, message = "Username length must be between 3 and 32 characters")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Username may contain only letters, numbers, and underscores")
    private String username;

    /**
     * Raw password.
     */
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, max = 128, message = "Password length must be between 8 and 128 characters")
    private String password;

    /**
     * Nickname displayed in the system.
     */
    @Size(max = 64, message = "Nickname cannot exceed 64 characters")
    private String nickname;

    /**
     * Email address.
     */
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email address is invalid")
    @Size(max = 128, message = "Email cannot exceed 128 characters")
    private String email;
}
