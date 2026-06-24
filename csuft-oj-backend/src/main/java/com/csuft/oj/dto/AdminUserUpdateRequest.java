package com.csuft.oj.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminUserUpdateRequest {

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "STUDENT|TEACHER|ADMIN", message = "Role is invalid")
    private String role;

    @NotNull(message = "Status is required")
    @Min(value = 0, message = "Status is invalid")
    @Max(value = 1, message = "Status is invalid")
    private Integer status;
}
