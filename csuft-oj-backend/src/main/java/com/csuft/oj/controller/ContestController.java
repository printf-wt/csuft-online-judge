package com.csuft.oj.controller;

import com.csuft.oj.audit.AuditLog;
import com.csuft.oj.dto.ContestCreateRequest;
import com.csuft.oj.dto.ContestProblemBatchBindRequest;
import com.csuft.oj.dto.ContestUpdateRequest;
import com.csuft.oj.security.SecurityUtils;
import com.csuft.oj.service.ContestService;
import com.csuft.oj.vo.ApiResponse;
import com.csuft.oj.vo.ContestProblemVO;
import com.csuft.oj.vo.ContestRanklistVO;
import com.csuft.oj.vo.ContestRegistrationVO;
import com.csuft.oj.vo.ContestVO;
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

import java.util.List;

/**
 * Contest management, registration, and ranklist APIs.
 */
@RestController
public class ContestController {

    private final ContestService contestService;

    public ContestController(ContestService contestService) {
        this.contestService = contestService;
    }

    /**
     * Lists contests. Public users only see public enabled contests.
     */
    @GetMapping("/api/contests")
    public ApiResponse<PageResult<ContestVO>> listContests(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.success(contestService.listContests(page, size, SecurityUtils.isTeacherOrAdmin()));
    }

    /**
     * Gets contest detail.
     */
    @GetMapping("/api/contests/{contestId}")
    public ApiResponse<ContestVO> getContest(@PathVariable Long contestId) {
        return ApiResponse.success(contestService.getContest(contestId, SecurityUtils.isTeacherOrAdmin()));
    }

    /**
     * Lists contest problem bindings.
     */
    @GetMapping("/api/contests/{contestId}/problems")
    public ApiResponse<List<ContestProblemVO>> listContestProblems(@PathVariable Long contestId) {
        return ApiResponse.success(contestService.listContestProblems(contestId, SecurityUtils.isTeacherOrAdmin()));
    }

    /**
     * Registers current user for a contest.
     */
    @AuditLog(action = "REGISTER_CONTEST")
    @PostMapping("/api/contests/{contestId}/register")
    public ApiResponse<ContestRegistrationVO> register(@PathVariable Long contestId) {
        return ApiResponse.success(contestService.register(
                contestId,
                SecurityUtils.currentUserIdOrNull(),
                SecurityUtils.isTeacherOrAdmin()));
    }

    /**
     * Calculates real-time contest ranklist.
     */
    @GetMapping("/api/contests/{contestId}/ranklist")
    public ApiResponse<ContestRanklistVO> ranklist(@PathVariable Long contestId) {
        return ApiResponse.success(contestService.ranklist(contestId, SecurityUtils.isTeacherOrAdmin()));
    }

    /**
     * Creates a contest. Requires TEACHER or ADMIN.
     */
    @AuditLog(action = "CREATE_CONTEST")
    @PostMapping("/api/teacher/contests")
    public ApiResponse<ContestVO> createContest(@Valid @RequestBody ContestCreateRequest request) {
        return ApiResponse.success(contestService.createContest(request, SecurityUtils.currentUserIdOrNull()));
    }

    /**
     * Updates a contest. Requires TEACHER or ADMIN.
     */
    @AuditLog(action = "UPDATE_CONTEST")
    @PutMapping("/api/teacher/contests/{contestId}")
    public ApiResponse<ContestVO> updateContest(
            @PathVariable Long contestId,
            @Valid @RequestBody ContestUpdateRequest request) {
        return ApiResponse.success(contestService.updateContest(contestId, request));
    }

    /**
     * Deletes a contest. Requires TEACHER or ADMIN.
     */
    @AuditLog(action = "DELETE_CONTEST")
    @DeleteMapping("/api/teacher/contests/{contestId}")
    public ApiResponse<Void> deleteContest(@PathVariable Long contestId) {
        contestService.deleteContest(contestId);
        return ApiResponse.success(null);
    }

    /**
     * Replaces contest problem bindings. Requires TEACHER or ADMIN.
     */
    @AuditLog(action = "BIND_CONTEST_PROBLEMS")
    @PostMapping("/api/teacher/contests/{contestId}/problems")
    public ApiResponse<List<ContestProblemVO>> bindProblems(
            @PathVariable Long contestId,
            @Valid @RequestBody ContestProblemBatchBindRequest request) {
        return ApiResponse.success(contestService.bindProblems(contestId, request));
    }
}
