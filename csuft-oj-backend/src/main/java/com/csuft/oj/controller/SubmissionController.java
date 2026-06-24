package com.csuft.oj.controller;

import com.csuft.oj.dto.SubmissionCreateRequest;
import com.csuft.oj.security.RateLimitService;
import com.csuft.oj.security.SecurityUtils;
import com.csuft.oj.service.SubmissionService;
import com.csuft.oj.vo.ApiResponse;
import com.csuft.oj.vo.PageResult;
import com.csuft.oj.vo.SubmissionCreateVO;
import com.csuft.oj.vo.SubmissionVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Submission APIs.
 */
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final RateLimitService rateLimitService;
    private final int submissionLimit;
    private final long submissionWindowSeconds;

    public SubmissionController(
            SubmissionService submissionService,
            RateLimitService rateLimitService,
            @Value("${csuft-oj.rate-limit.submissions:30}") int submissionLimit,
            @Value("${csuft-oj.rate-limit.submission-window-seconds:60}") long submissionWindowSeconds) {
        this.submissionService = submissionService;
        this.rateLimitService = rateLimitService;
        this.submissionLimit = submissionLimit;
        this.submissionWindowSeconds = submissionWindowSeconds;
    }

    /**
     * Creates a pending submission and dispatches it to the asynchronous judge queue.
     */
    @PostMapping
    public ApiResponse<SubmissionCreateVO> submit(@Valid @RequestBody SubmissionCreateRequest request) {
        Long userId = SecurityUtils.currentUserIdOrNull();
        rateLimitService.check(
                "submission:" + userId,
                submissionLimit,
                java.time.Duration.ofSeconds(submissionWindowSeconds));
        return ApiResponse.success(submissionService.submit(request, userId));
    }

    /**
     * Lists submissions with optional filters.
     */
    @GetMapping
    public ApiResponse<PageResult<SubmissionVO>> listSubmissions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Long contestId) {
        boolean canViewAll = SecurityUtils.isTeacherOrAdmin();
        return ApiResponse.success(submissionService.listSubmissions(
                page,
                size,
                problemId,
                userId,
                status,
                language,
                contestId,
                SecurityUtils.currentUserIdOrNull(),
                canViewAll));
    }

    /**
     * Gets one submission detail.
     */
    @GetMapping("/{id}")
    public ApiResponse<SubmissionVO> getSubmission(@PathVariable Long id) {
        return ApiResponse.success(submissionService.getSubmission(
                id,
                SecurityUtils.currentUserIdOrNull(),
                SecurityUtils.isTeacherOrAdmin()));
    }
}
