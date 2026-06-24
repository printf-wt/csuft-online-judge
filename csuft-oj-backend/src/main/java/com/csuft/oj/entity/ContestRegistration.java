package com.csuft.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 比赛报名表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_contest_registration")
public class ContestRegistration {

    /**
     * 报名ID，MyBatis-Plus 使用的自增主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 比赛ID，逻辑关联 tb_contest.id；与 userId 构成复合业务主键。
     */
    private Long contestId;

    /**
     * 用户ID，逻辑关联 tb_user.id；与 contestId 构成复合业务主键。
     */
    private Long userId;

    /**
     * 报名状态，如 REGISTERED、CANCELLED。
     */
    private String status;

    /**
     * 报名时间。
     */
    private LocalDateTime registeredAt;
}
