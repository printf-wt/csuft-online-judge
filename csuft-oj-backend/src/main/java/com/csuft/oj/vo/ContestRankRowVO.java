package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One row in a contest ranklist.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestRankRowVO {

    /**
     * Rank number.
     */
    private Long rank;

    /**
     * User ID.
     */
    private Long userId;

    /**
     * Username.
     */
    private String username;

    /**
     * Nickname.
     */
    private String nickname;

    /**
     * Accepted problem count for ACM mode.
     */
    private Integer acceptedCount;

    /**
     * Total penalty minutes for ACM mode.
     */
    private Long totalPenaltyMinutes;

    /**
     * Total score for IOI mode.
     */
    private Integer totalScore;

    /**
     * Per-problem rank details.
     */
    private List<ContestRankProblemVO> problems;
}
