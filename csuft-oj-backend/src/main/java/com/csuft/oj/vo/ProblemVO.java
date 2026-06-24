package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Problem response data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemVO {

    /**
     * Problem ID.
     */
    private Long id;

    /**
     * Problem title.
     */
    private String title;

    /**
     * Full problem statement.
     */
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
    private String difficulty;

    /**
     * Time limit in milliseconds.
     */
    private Integer timeLimitMs;

    /**
     * Memory limit in KB.
     */
    private Integer memoryLimitKb;

    /**
     * Author user ID.
     */
    private Long authorId;

    /**
     * Visibility flag: 1 visible, 0 hidden.
     */
    private Integer isVisible;

    /**
     * Accepted submission count.
     */
    private Integer acceptedCount;

    /**
     * Total submission count.
     */
    private Integer submitCount;

    /**
     * Current user's status on this problem: ACCEPTED, WRONG_ANSWER, or NOT_SUBMITTED.
     */
    private String submissionStatus;

    /**
     * Creation time.
     */
    private LocalDateTime createdAt;

    /**
     * Last update time.
     */
    private LocalDateTime updatedAt;
}
