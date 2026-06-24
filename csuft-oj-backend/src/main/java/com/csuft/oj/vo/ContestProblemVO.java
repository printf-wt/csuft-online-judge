package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Contest problem binding response data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestProblemVO {

    /**
     * Binding ID.
     */
    private Long id;

    /**
     * Contest ID.
     */
    private Long contestId;

    /**
     * Problem ID.
     */
    private Long problemId;

    /**
     * Problem alias, such as A, B, C.
     */
    private String alias;

    /**
     * Display order.
     */
    private Integer sortOrder;

    /**
     * Full score or weight.
     */
    private Integer score;

    /**
     * Creation time.
     */
    private LocalDateTime createdAt;
}
