package com.csuft.oj.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request payload for updating a contest.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestUpdateRequest {

    /**
     * Contest title.
     */
    @Size(min = 1, max = 255, message = "Contest title length must be between 1 and 255 characters")
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
    private LocalDateTime startTime;

    /**
     * Contest end time.
     */
    private LocalDateTime endTime;

    /**
     * Whether the contest is public: 1 public, 0 private.
     */
    @Min(value = 0, message = "isPublic must be 0 or 1")
    @Max(value = 1, message = "isPublic must be 0 or 1")
    private Integer isPublic;

    /**
     * Contest status: 1 normal, 0 disabled.
     */
    @Min(value = 0, message = "status must be 0 or 1")
    @Max(value = 1, message = "status must be 0 or 1")
    private Integer status;
}
