package com.csuft.oj.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    @NotBlank(message = "Nickname cannot be empty")
    @Size(max = 64, message = "Nickname cannot exceed 64 characters")
    private String nickname;

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email address is invalid")
    @Size(max = 128, message = "Email cannot exceed 128 characters")
    private String email;
}
