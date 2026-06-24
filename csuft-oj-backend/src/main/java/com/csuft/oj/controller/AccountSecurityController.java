package com.csuft.oj.controller;

import com.csuft.oj.audit.AuditLog;
import com.csuft.oj.dto.ChangePasswordRequest;
import com.csuft.oj.security.SecurityUtils;
import com.csuft.oj.service.AccountSecurityService;
import com.csuft.oj.vo.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class AccountSecurityController {

    private final AccountSecurityService accountSecurityService;

    public AccountSecurityController(AccountSecurityService accountSecurityService) {
        this.accountSecurityService = accountSecurityService;
    }

    @AuditLog(action = "CHANGE_PASSWORD")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        accountSecurityService.changePassword(SecurityUtils.currentUserIdOrNull(), request);
        return ApiResponse.success(null);
    }
}
