package com.csuft.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 审计日志表实体。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tb_audit_log")
public class AuditLog {

    /**
     * 审计日志ID。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作人用户ID，逻辑关联 tb_user.id。
     */
    private Long operatorId;

    /**
     * 操作类型。
     */
    private String action;

    /**
     * 操作对象类型。
     */
    private String targetType;

    /**
     * 操作对象ID。
     */
    private Long targetId;

    /**
     * 客户端IP地址。
     */
    private String ipAddress;

    /**
     * 客户端 User-Agent。
     */
    private String userAgent;

    /**
     * 操作详情JSON字符串。
     */
    private String detail;

    /**
     * 操作时间。
     */
    private LocalDateTime createdAt;
}
