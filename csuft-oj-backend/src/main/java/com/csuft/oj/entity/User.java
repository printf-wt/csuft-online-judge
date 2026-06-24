package com.csuft.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_user")
public class User {

    /**
     * 用户ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名。
     */
    private String username;

    /**
     * 密码哈希。
     */
    private String passwordHash;

    /**
     * 昵称。
     */
    private String nickname;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 用户角色，如 USER、ADMIN。
     */
    private String role;

    /**
     * 全局AC题数。
     */
    private Integer globalAcCount;

    /**
     * 提交总次数。
     */
    private Integer submitCount;

    /**
     * 用户状态：1正常，0禁用。
     */
    private Integer status;

    /**
     * Increments whenever all existing access tokens must become invalid.
     */
    private Integer tokenVersion;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
