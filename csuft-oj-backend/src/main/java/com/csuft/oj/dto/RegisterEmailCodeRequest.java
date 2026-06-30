package com.csuft.oj.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for sending a registration email verification code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterEmailCodeRequest {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email address is invalid")
    @Size(max = 128, message = "Email cannot exceed 128 characters")
    private String email;
}
