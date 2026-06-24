package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Contest response data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestVO {

    /**
     * Contest ID.
     */
    private Long id;

    /**
     * Contest title.
     */
    private String title;

    /**
     * Contest description.
     */
    private String description;

    /**
     * Rule type: ACM or IOI.
     */
    private String ruleType;

    /**
     * Contest start time.
     */
    private LocalDateTime startTime;

    /**
     * Contest end time.
     */
    private LocalDateTime endTime;

    /**
     * Whether public: 1 public, 0 private.
     */
    private Integer isPublic;

    /**
     * Contest status: 1 normal, 0 disabled.
     */
    private Integer status;

    /**
     * Creator user ID.
     */
    private Long createdBy;

    /**
     * Creation time.
     */
    private LocalDateTime createdAt;

    /**
     * Last update time.
     */
    private LocalDateTime updatedAt;
}
