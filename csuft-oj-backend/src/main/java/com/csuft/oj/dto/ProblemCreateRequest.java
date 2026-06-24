package com.csuft.oj.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for creating a problem.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemCreateRequest {

    /**
     * Problem title.
     */
    @NotBlank(message = "Problem title cannot be empty")
    @Size(max = 255, message = "Problem title cannot exceed 255 characters")
    private String title;

    /**
     * Full problem statement.
     */
    @NotBlank(message = "Problem description cannot be empty")
    @Size(max = 1000000, message = "Problem description is too long")
    private String description;

    /**
     * Input format description.
     */
    private String inputDescription;

    /**
     * Output format description.
     */
    private String outputDescription;

    /**
     * Sample input text.
     */
    private String sampleInput;

    /**
     * Sample output text.
     */
    private String sampleOutput;

    /**
     * Difficulty: EASY, MEDIUM, or HARD.
     */
    @Pattern(regexp = "(?i)EASY|MEDIUM|HARD", message = "Difficulty must be EASY, MEDIUM, or HARD")
    private String difficulty;

    /**
     * Time limit in milliseconds.
     */
    @Min(value = 100, message = "Time limit must be at least 100 ms")
    @Max(value = 60000, message = "Time limit cannot exceed 60000 ms")
    private Integer timeLimitMs;

    /**
     * Memory limit in KB.
     */
    @Min(value = 16384, message = "Memory limit must be at least 16384 KB")
    @Max(value = 2097152, message = "Memory limit cannot exceed 2097152 KB")
    private Integer memoryLimitKb;

    /**
     * Whether the problem is visible to public users.
     */
    @Min(value = 0, message = "Visibility must be 0 or 1")
    @Max(value = 1, message = "Visibility must be 0 or 1")
    private Integer isVisible;
}
