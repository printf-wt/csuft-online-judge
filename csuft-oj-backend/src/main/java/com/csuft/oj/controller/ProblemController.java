package com.csuft.oj.controller;

import com.csuft.oj.audit.AuditLog;
import com.csuft.oj.dto.ProblemCreateRequest;
import com.csuft.oj.dto.ProblemUpdateRequest;
import com.csuft.oj.security.SecurityUtils;
import com.csuft.oj.service.ProblemService;
import com.csuft.oj.vo.ApiResponse;
import com.csuft.oj.vo.PageResult;
import com.csuft.oj.vo.ProblemVO;
import com.csuft.oj.vo.TestCaseUploadVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Problem APIs, including public reads and teacher/admin management.
 */
@RestController
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    /**
     * Lists problems. Hidden problems are visible only to teachers and administrators.
     */
    @GetMapping("/api/problems")
    public ApiResponse<PageResult<ProblemVO>> listProblems(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer visible) {
        return ApiResponse.success(problemService.listProblems(
                page,
                size,
                keyword,
                visible,
                SecurityUtils.currentUserIdOrNull(),
                SecurityUtils.canViewHiddenProblems()));
    }

    /**
     * Gets problem detail. Hidden problems are visible only to teachers and administrators.
     */
    @GetMapping("/api/problems/{id}")
    public ApiResponse<ProblemVO> getProblem(@PathVariable Long id) {
        return ApiResponse.success(problemService.getProblem(
                id,
                SecurityUtils.currentUserIdOrNull(),
                SecurityUtils.canViewHiddenProblems()));
    }

    /**
     * Creates a problem. Requires TEACHER or ADMIN.
     */
    @AuditLog(action = "CREATE_PROBLEM")
    @PostMapping("/api/teacher/problems")
    public ApiResponse<ProblemVO> createProblem(@Valid @RequestBody ProblemCreateRequest request) {
        return ApiResponse.success(problemService.createProblem(request, SecurityUtils.currentUserIdOrNull()));
    }

    /**
     * Updates a problem. Requires TEACHER or ADMIN.
     */
    @AuditLog(action = "UPDATE_PROBLEM")
    @PutMapping("/api/teacher/problems/{id}")
    public ApiResponse<ProblemVO> updateProblem(
            @PathVariable Long id,
            @Valid @RequestBody ProblemUpdateRequest request) {
        return ApiResponse.success(problemService.updateProblem(id, request));
    }

    /**
     * Deletes a problem. Requires TEACHER or ADMIN.
     */
    @AuditLog(action = "DELETE_PROBLEM")
    @DeleteMapping("/api/teacher/problems/{id}")
    public ApiResponse<Void> deleteProblem(@PathVariable Long id) {
        problemService.deleteProblem(id);
        return ApiResponse.success(null);
    }

    /**
     * Uploads zipped test cases. Requires TEACHER or ADMIN.
     */
    @AuditLog(action = "UPLOAD_TEST_CASES")
    @PostMapping("/api/teacher/problems/{problemId}/testcases")
    public ApiResponse<TestCaseUploadVO> uploadTestCases(
            @PathVariable Long problemId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(problemService.uploadTestCases(problemId, file));
    }
}
