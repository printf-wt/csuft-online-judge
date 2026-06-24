package com.csuft.oj.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload for replacing contest problem bindings.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestProblemBatchBindRequest {

    /**
     * Problems to bind to the contest.
     */
    @NotEmpty(message = "Contest problems cannot be empty")
    @Size(max = 100, message = "A contest cannot contain more than 100 problems")
    private List<@Valid ContestProblemBindRequest> problems;
}
