package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Contest registration response data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContestRegistrationVO {

    /**
     * Registration ID.
     */
    private Long id;

    /**
     * Contest ID.
     */
    private Long contestId;

    /**
     * User ID.
     */
    private Long userId;

    /**
     * Registration status.
     */
    private String status;

    /**
     * Registration time.
     */
    private LocalDateTime registeredAt;
}
