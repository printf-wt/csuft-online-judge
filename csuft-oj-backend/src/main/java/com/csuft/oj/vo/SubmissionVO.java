package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Submission response data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionVO {

    /**
     * Submission ID.
     */
    private Long id;

    /**
     * Submitter user ID.
     */
    private Long userId;

    /**
     * Problem ID.
     */
    private Long problemId;

    /**
     * Contest ID, or null for practice submissions.
     */
    private Long contestId;

    /**
     * Programming language.
     */
    private String language;

    /**
     * Source code. Hidden from unauthorized users.
     */
    private String code;

    /**
     * Code length.
     */
    private Integer codeLength;

    /**
     * Judge status.
     */
    private String status;

    /**
     * Score.
     */
    private Integer score;

    /**
     * Max execution time in milliseconds.
     */
    private Integer timeUsedMs;

    /**
     * Max memory usage in KB.
     */
    private Integer memoryUsedKb;

    /**
     * Judge message.
     */
    private String judgeMessage;

    /**
     * Error log. Visible only to teachers and administrators.
     */
    private String errorLog;

    /**
     * Submit time.
     */
    private LocalDateTime createdAt;

    /**
     * Judge finish time.
     */
    private LocalDateTime judgedAt;
}
