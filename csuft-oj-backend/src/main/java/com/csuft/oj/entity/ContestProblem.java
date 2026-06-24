package com.csuft.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 比赛题目关联表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_contest_problem")
public class ContestProblem {

    /**
     * 比赛题目关联ID，MyBatis-Plus 使用的自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 比赛ID，逻辑关联 tb_contest.id；与 problemId 构成复合业务主键。
     */
    private Long contestId;

    /**
     * 题目ID，逻辑关联 tb_problem.id；与 contestId 构成复合业务主键。
     */
    private Long problemId;

    /**
     * 题目别名，如 A、B、C。
     */
    private String alias;

    /**
     * 题目排序。
     */
    private Integer sortOrder;

    /**
     * 题目分值。
     */
    private Integer score;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
}
