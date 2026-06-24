package com.csuft.oj.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for binding one problem to a contest.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestProblemBindRequest {

    /**
     * Problem ID.
     */
    @NotNull(message = "Problem ID cannot be empty")
    @Positive(message = "Problem ID must be positive")
    private Long problemId;

    /**
     * Problem alias in the contest, such as A, B, C.
     */
    @NotBlank(message = "Problem alias cannot be empty")
    @Size(max = 16, message = "Problem alias cannot exceed 16 characters")
    private String alias;

    /**
     * Display order.
     */
    @NotNull(message = "Problem sort order cannot be empty")
    @PositiveOrZero(message = "Problem sort order must be non-negative")
    private Integer sortOrder;

    /**
     * Full score or weight for this contest problem.
     */
    @Positive(message = "Problem score must be positive")
    private Integer score;
}
