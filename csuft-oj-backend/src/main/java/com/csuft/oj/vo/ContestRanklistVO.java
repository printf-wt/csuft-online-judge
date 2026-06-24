package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Contest ranklist response data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestRanklistVO {

    /**
     * Contest ID.
     */
    private Long contestId;

    /**
     * Rule type: ACM or IOI.
     */
    private String ruleType;

    /**
     * Contest problem headers.
     */
    private List<ContestProblemVO> problems;

    /**
     * Rank rows.
     */
    private List<ContestRankRowVO> rows;
}
