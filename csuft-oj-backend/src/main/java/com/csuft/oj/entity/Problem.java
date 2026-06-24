package com.csuft.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 题目表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_problem")
public class Problem {

    /**
     * 题目ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 题目标题。
     */
    private String title;

    /**
     * 题目描述。
     */
    private String description;

    /**
     * 输入描述。
     */
    private String inputDescription;

    /**
     * 输出描述。
     */
    private String outputDescription;

    /**
     * 样例输入。
     */
    private String sampleInput;

    /**
     * 样例输出。
     */
    private String sampleOutput;

    /**
     * 题目难度，如 EASY、MEDIUM、HARD。
     */
    private String difficulty;

    /**
     * 时间限制，单位毫秒。
     */
    private Integer timeLimitMs;

    /**
     * 内存限制，单位KB。
     */
    private Integer memoryLimitKb;

    /**
     * 作者用户ID，逻辑关联 tb_user.id。
     */
    private Long authorId;

    /**
     * 是否可见：1可见，0隐藏。
     */
    private Integer isVisible;

    /**
     * AC次数。
     */
    private Integer acceptedCount;

    /**
     * 提交次数。
     */
    private Integer submitCount;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
