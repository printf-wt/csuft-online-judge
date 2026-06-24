package com.csuft.oj.controller;

import com.csuft.oj.audit.AuditLog;
import com.csuft.oj.dto.UserProfileUpdateRequest;
import com.csuft.oj.security.SecurityUtils;
import com.csuft.oj.service.UserProfileService;
import com.csuft.oj.vo.ApiResponse;
import com.csuft.oj.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public ApiResponse<UserProfileVO> profile() {
        return ApiResponse.success(userProfileService.getProfile(SecurityUtils.currentUserIdOrNull()));
    }

    @AuditLog(action = "UPDATE_PROFILE")
    @PutMapping
    public ApiResponse<UserProfileVO> updateProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success(userProfileService.updateProfile(SecurityUtils.currentUserIdOrNull(), request));
    }
}
