package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response data returned immediately after submitting code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionCreateVO {

    /**
     * Generated submission ID.
     */
    private Long submissionId;

    /**
     * Initial judge status.
     */
    private String status;
}
