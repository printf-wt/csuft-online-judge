package com.csuft.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 提交记录表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_submission")
public class Submission {

    /**
     * 提交ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 提交用户ID，逻辑关联 tb_user.id。
     */
    private Long userId;

    /**
     * 题目ID，逻辑关联 tb_problem.id。
     */
    private Long problemId;

    /**
     * 比赛ID，逻辑关联 tb_contest.id；非比赛提交为空。
     */
    private Long contestId;

    /**
     * 提交语言。
     */
    private String language;

    /**
     * 提交代码。
     */
    private String code;

    /**
     * 代码长度。
     */
    private Integer codeLength;

    /**
     * 判题状态，如 PENDING、JUDGING、ACCEPTED、WRONG_ANSWER。
     */
    private String status;

    /**
     * 得分，ACM赛制通常为0或100。
     */
    private Integer score;

    /**
     * 运行耗时，单位毫秒。
     */
    private Integer timeUsedMs;

    /**
     * 运行内存，单位KB。
     */
    private Integer memoryUsedKb;

    /**
     * 判题信息。
     */
    private String judgeMessage;

    /**
     * Compilation or runtime error log.
     */
    @TableField("error_log")
    private String errorLog;

    /**
     * 提交时间。
     */
    private LocalDateTime createdAt;

    /**
     * 判题完成时间。
     */
    private LocalDateTime judgedAt;
}
