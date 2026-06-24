package com.csuft.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Login request payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /**
     * Username.
     */
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 64, message = "Username length must be between 3 and 64 characters")
    private String username;

    /**
     * Raw password.
     */
    @NotBlank(message = "Password cannot be empty")
    @Size(min = 8, max = 128, message = "Password length must be between 8 and 128 characters")
    private String password;
}
