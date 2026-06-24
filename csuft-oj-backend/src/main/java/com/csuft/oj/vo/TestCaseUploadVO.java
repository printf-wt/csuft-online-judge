package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Test case upload result.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseUploadVO {

    /**
     * Problem ID.
     */
    private Long problemId;

    /**
     * Physical directory where files were extracted.
     */
    private String uploadDir;

    /**
     * Number of valid input/output pairs.
     */
    private Integer count;

    /**
     * Saved test case metadata.
     */
    private List<TestCaseVO> testCases;
}
