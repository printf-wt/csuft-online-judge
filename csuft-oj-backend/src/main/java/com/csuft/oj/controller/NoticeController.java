package com.csuft.oj.controller;

import com.csuft.oj.audit.AuditLog;
import com.csuft.oj.dto.NoticeCreateRequest;
import com.csuft.oj.dto.NoticeUpdateRequest;
import com.csuft.oj.security.SecurityUtils;
import com.csuft.oj.service.NoticeService;
import com.csuft.oj.vo.ApiResponse;
import com.csuft.oj.vo.NoticeVO;
import com.csuft.oj.vo.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/api/notices")
    public ApiResponse<PageResult<NoticeVO>> publicNotices(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.success(noticeService.list(page, size, false));
    }

    @GetMapping("/api/notices/{id}")
    public ApiResponse<NoticeVO> publicNotice(@PathVariable Long id) {
        return ApiResponse.success(noticeService.get(id, false));
    }

    @GetMapping("/api/admin/notices")
    public ApiResponse<PageResult<NoticeVO>> adminNotices(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(noticeService.list(page, size, true));
    }

    @AuditLog(action = "CREATE_NOTICE")
    @PostMapping("/api/admin/notices")
    public ApiResponse<NoticeVO> create(@Valid @RequestBody NoticeCreateRequest request) {
        return ApiResponse.success(noticeService.create(request, SecurityUtils.currentUserIdOrNull()));
    }

    @AuditLog(action = "UPDATE_NOTICE")
    @PutMapping("/api/admin/notices/{id}")
    public ApiResponse<NoticeVO> update(
            @PathVariable Long id,
            @Valid @RequestBody NoticeUpdateRequest request) {
        return ApiResponse.success(noticeService.update(id, request));
    }

    @AuditLog(action = "DELETE_NOTICE")
    @DeleteMapping("/api/admin/notices/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ApiResponse.success(null);
    }
}
