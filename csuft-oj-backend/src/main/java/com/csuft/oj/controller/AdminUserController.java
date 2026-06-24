package com.csuft.oj.controller;

import com.csuft.oj.audit.AuditLog;
import com.csuft.oj.dto.AdminUserUpdateRequest;
import com.csuft.oj.security.SecurityUtils;
import com.csuft.oj.service.AdminUserService;
import com.csuft.oj.vo.AdminUserVO;
import com.csuft.oj.vo.ApiResponse;
import com.csuft.oj.vo.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminUserVO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(adminUserService.list(page, size, keyword, role, status));
    }

    @AuditLog(action = "UPDATE_USER_ACCESS")
    @PutMapping("/{id}")
    public ApiResponse<AdminUserVO> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequest request) {
        return ApiResponse.success(adminUserService.update(SecurityUtils.currentUserIdOrNull(), id, request));
    }
}
