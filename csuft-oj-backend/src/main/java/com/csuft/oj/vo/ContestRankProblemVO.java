package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Per-problem ranklist detail for one user.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestRankProblemVO {

    /**
     * Contest problem alias, such as A, B, C.
     */
    private String alias;

    /**
     * Problem ID.
     */
    private Long problemId;

    /**
     * Whether this problem has been accepted in ACM mode.
     */
    private Boolean accepted;

    /**
     * Wrong attempts before first AC, excluding compile errors.
     */
    private Integer wrongAttemptsBeforeAc;

    /**
     * ACM penalty minutes for this problem. Null if not accepted.
     */
    private Long penaltyMinutes;

    /**
     * IOI best score for this problem.
     */
    private Integer score;

    /**
     * First accepted time or best score submission time.
     */
    private LocalDateTime solvedAt;
}
