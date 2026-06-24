package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Test case response data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseVO {

    /**
     * Test case ID.
     */
    private Long id;

    /**
     * Problem ID.
     */
    private Long problemId;

    /**
     * Input file path or file name.
     */
    private String inputPath;

    /**
     * Output file path or file name.
     */
    private String outputPath;

    /**
     * Input file 2KB preview.
     */
    private String inputPreview;

    /**
     * Output file 2KB preview.
     */
    private String outputPreview;

    /**
     * Test case score.
     */
    private Integer score;

    /**
     * Sort order.
     */
    private Integer sortOrder;

    /**
     * Creation time.
     */
    private LocalDateTime createdAt;
}
