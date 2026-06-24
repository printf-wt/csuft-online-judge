package com.csuft.oj.controller;

import com.csuft.oj.service.AdminSystemService;
import com.csuft.oj.vo.ApiResponse;
import com.csuft.oj.vo.AuditLogVO;
import com.csuft.oj.vo.PageResult;
import com.csuft.oj.vo.SystemMonitorVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin system monitor and audit APIs.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminSystemController {

    private final AdminSystemService adminSystemService;

    public AdminSystemController(AdminSystemService adminSystemService) {
        this.adminSystemService = adminSystemService;
    }

    @GetMapping("/system/monitor")
    public ApiResponse<SystemMonitorVO> monitor() {
        return ApiResponse.success(adminSystemService.monitor());
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResult<AuditLogVO>> auditLogs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String action) {
        return ApiResponse.success(adminSystemService.auditLogs(page, size, action));
    }
}
