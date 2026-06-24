package com.csuft.oj.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request payload for creating a contest.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestCreateRequest {

    /**
     * Contest title.
     */
    @NotBlank(message = "Contest title cannot be empty")
    @Size(max = 255, message = "Contest title cannot exceed 255 characters")
    private String title;

    /**
     * Contest description.
     */
    @Size(max = 65535, message = "Contest description is too long")
    private String description;

    /**
     * Rule type: ACM or IOI.
     */
    @Pattern(regexp = "(?i)ACM|IOI", message = "Rule type must be ACM or IOI")
    private String ruleType;

    /**
     * Contest start time.
     */
    @NotNull(message = "Contest start time cannot be empty")
    private LocalDateTime startTime;

    /**
     * Contest end time.
     */
    @NotNull(message = "Contest end time cannot be empty")
    private LocalDateTime endTime;

    /**
     * Whether the contest is public: 1 public, 0 private.
     */
    @Min(value = 0, message = "isPublic must be 0 or 1")
    @Max(value = 1, message = "isPublic must be 0 or 1")
    private Integer isPublic;
}
