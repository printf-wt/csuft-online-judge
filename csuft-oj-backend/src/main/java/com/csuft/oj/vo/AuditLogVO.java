package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit log response row.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogVO {

    private Long id;
    private Long operatorId;
    private String action;
    private String targetType;
    private Long targetId;
    private String ipAddress;
    private String userAgent;
    private String detail;
    private LocalDateTime createdAt;
}
