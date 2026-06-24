package com.csuft.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 比赛表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_contest")
public class Contest {

    /**
     * 比赛ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 比赛标题。
     */
    private String title;

    /**
     * 比赛描述。
     */
    private String description;

    /**
     * 赛制类型，如 ACM、IOI。
     */
    private String ruleType;

    /**
     * 比赛开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 比赛结束时间。
     */
    private LocalDateTime endTime;

    /**
     * 是否公开：1公开，0私有。
     */
    private Integer isPublic;

    /**
     * 比赛状态：1正常，0停用。
     */
    private Integer status;

    /**
     * 创建者用户ID，逻辑关联 tb_user.id。
     */
    private Long createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
