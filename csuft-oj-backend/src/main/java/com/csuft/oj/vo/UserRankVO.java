package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Global ranklist row.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRankVO {

    /**
     * Rank number in the current global ordering.
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
     * Number of solved problems.
     */
    private Integer solvedCount;

    /**
     * Number of total submissions.
     */
    private Integer submitCount;
}
