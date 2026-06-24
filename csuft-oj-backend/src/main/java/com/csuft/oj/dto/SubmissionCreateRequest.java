package com.csuft.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a code submission.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionCreateRequest {

    /**
     * Problem ID.
     */
    @NotNull(message = "Problem ID cannot be empty")
    @Positive(message = "Problem ID must be positive")
    private Long problemId;

    /**
     * Source code.
     */
    @NotBlank(message = "Code cannot be empty")
    @Size(max = 131072, message = "Code exceeds the maximum allowed length")
    private String code;

    /**
     * Programming language, such as C++, Java, Python, or Go.
     */
    @NotBlank(message = "Language cannot be empty")
    @Size(max = 32, message = "Language cannot exceed 32 characters")
    private String language;

    /**
     * Contest ID. Null means normal practice submission.
     */
    @Positive(message = "Contest ID must be positive")
    private Long contestId;
}
