package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csuft.oj.dto.SubmissionCreateRequest;
import com.csuft.oj.entity.Contest;
import com.csuft.oj.entity.ContestProblem;
import com.csuft.oj.entity.ContestRegistration;
import com.csuft.oj.entity.Problem;
import com.csuft.oj.entity.Submission;
import com.csuft.oj.entity.User;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.judge.JudgeTaskPublisher;
import com.csuft.oj.mapper.ContestMapper;
import com.csuft.oj.mapper.ContestProblemMapper;
import com.csuft.oj.mapper.ContestRegistrationMapper;
import com.csuft.oj.mapper.ProblemMapper;
import com.csuft.oj.mapper.SubmissionMapper;
import com.csuft.oj.mapper.UserMapper;
import com.csuft.oj.vo.PageResult;
import com.csuft.oj.vo.SubmissionCreateVO;
import com.csuft.oj.vo.SubmissionVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Submission creation and query use cases.
 */
@Service
public class SubmissionService {

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("C++", "JAVA", "PYTHON", "GO");

    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final UserMapper userMapper;
    private final ContestMapper contestMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ContestRegistrationMapper contestRegistrationMapper;
    private final JudgeTaskPublisher judgeTaskPublisher;
    private final int maxCodeLength;

    public SubmissionService(
            SubmissionMapper submissionMapper,
            ProblemMapper problemMapper,
            UserMapper userMapper,
            ContestMapper contestMapper,
            ContestProblemMapper contestProblemMapper,
            ContestRegistrationMapper contestRegistrationMapper,
            JudgeTaskPublisher judgeTaskPublisher,
            @Value("${csuft-oj.submission.max-code-length:131072}") int maxCodeLength) {
        this.submissionMapper = submissionMapper;
        this.problemMapper = problemMapper;
        this.userMapper = userMapper;
        this.contestMapper = contestMapper;
        this.contestProblemMapper = contestProblemMapper;
        this.contestRegistrationMapper = contestRegistrationMapper;
        this.judgeTaskPublisher = judgeTaskPublisher;
        this.maxCodeLength = maxCodeLength;
    }

    /**
     * Creates a pending submission and immediately enqueues it for asynchronous judging.
     */
    @Transactional(rollbackFor = Exception.class)
    public SubmissionCreateVO submit(SubmissionCreateRequest request, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(401, "Login required");
        }
        if (request == null) {
            throw new BusinessException("Submission request cannot be empty");
        }
        Long problemId = requireProblemId(request.getProblemId());
        String code = requireText(request.getCode(), "Code cannot be empty");
        if (code.length() > maxCodeLength) {
            throw new BusinessException("Code exceeds the maximum allowed length");
        }
        String language = normalizeLanguage(request.getLanguage());

        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BusinessException(404, "Problem not found");
        }
        LocalDateTime now = LocalDateTime.now();
        if (request.getContestId() == null && !Integer.valueOf(1).equals(problem.getIsVisible())) {
            throw new BusinessException(403, "Cannot submit to a hidden problem");
        }
        if (request.getContestId() != null) {
            validateContestSubmission(request.getContestId(), problemId, currentUserId, now);
        }

        Submission submission = new Submission();
        submission.setUserId(currentUserId);
        submission.setProblemId(problemId);
        submission.setContestId(request.getContestId());
        submission.setLanguage(language);
        submission.setCode(code);
        submission.setCodeLength(code.length());
        submission.setStatus("PENDING");
        submission.setScore(0);
        submission.setTimeUsedMs(0);
        submission.setMemoryUsedKb(0);
        submission.setJudgeMessage("Pending");
        submission.setErrorLog(null);
        submission.setCreatedAt(now);
        submission.setJudgedAt(null);

        submissionMapper.insert(submission);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, currentUserId)
                .setSql("submit_count = submit_count + 1"));
        problemMapper.update(null, new LambdaUpdateWrapper<Problem>()
                .eq(Problem::getId, problemId)
                .setSql("submit_count = submit_count + 1"));

        dispatchAfterCommit(submission.getId());
        return new SubmissionCreateVO(submission.getId(), submission.getStatus());
    }

    /**
     * Lists submissions with filters. Normal users are restricted to their own records.
     */
    public PageResult<SubmissionVO> listSubmissions(
            long page,
            long size,
            Long problemId,
            Long userId,
            String status,
            String language,
            Long contestId,
            Long currentUserId,
            boolean canViewAll) {
        Page<Submission> submissionPage = new Page<>(Math.max(page, 1L), Math.min(Math.max(size, 1L), 100L));
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();

        if (problemId != null) {
            wrapper.eq(Submission::getProblemId, problemId);
        }
        if (canViewAll) {
            if (userId != null) {
                wrapper.eq(Submission::getUserId, userId);
            }
        } else {
            wrapper.eq(Submission::getUserId, currentUserId);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(Submission::getStatus, status.trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(language)) {
            wrapper.eq(Submission::getLanguage, normalizeLanguage(language));
        }
        if (contestId != null) {
            wrapper.eq(Submission::getContestId, contestId);
        }
        wrapper.orderByDesc(Submission::getId);

        Page<Submission> result = submissionMapper.selectPage(submissionPage, wrapper);
        List<SubmissionVO> records = result.getRecords().stream()
                .map(submission -> toSubmissionVO(submission, false, false))
                .toList();
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), records);
    }

    /**
     * Gets one submission detail with code visibility checks.
     */
    public SubmissionVO getSubmission(Long id, Long currentUserId, boolean canViewAll) {
        if (id == null) {
            throw new BusinessException("Submission ID cannot be empty");
        }
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) {
            throw new BusinessException(404, "Submission not found");
        }
        if (!canViewAll && !ownsSubmission(submission, currentUserId)) {
            throw new BusinessException(403, "You can only view your own submission detail");
        }
        return toSubmissionVO(submission, true, canViewAll);
    }

    private SubmissionVO toSubmissionVO(Submission submission, boolean showCode, boolean showErrorLog) {
        return new SubmissionVO(
                submission.getId(),
                submission.getUserId(),
                submission.getProblemId(),
                submission.getContestId(),
                submission.getLanguage(),
                showCode ? submission.getCode() : null,
                submission.getCodeLength(),
                submission.getStatus(),
                submission.getScore(),
                submission.getTimeUsedMs(),
                submission.getMemoryUsedKb(),
                submission.getJudgeMessage(),
                showErrorLog ? submission.getErrorLog() : null,
                submission.getCreatedAt(),
                submission.getJudgedAt());
    }

    private boolean ownsSubmission(Submission submission, Long currentUserId) {
        return currentUserId != null && currentUserId.equals(submission.getUserId());
    }

    private Long requireProblemId(Long problemId) {
        if (problemId == null) {
            throw new BusinessException("Problem ID cannot be empty");
        }
        return problemId;
    }

    private void validateContestSubmission(
            Long contestId,
            Long problemId,
            Long userId,
            LocalDateTime submittedAt) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(404, "Contest not found");
        }
        if (!Integer.valueOf(1).equals(contest.getStatus())) {
            throw new BusinessException(403, "Contest is disabled");
        }
        if (submittedAt.isBefore(contest.getStartTime())) {
            throw new BusinessException(403, "Contest has not started");
        }
        if (submittedAt.isAfter(contest.getEndTime())) {
            throw new BusinessException(403, "Contest has ended");
        }

        Long bindingCount = contestProblemMapper.selectCount(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId)
                .eq(ContestProblem::getProblemId, problemId));
        if (bindingCount == null || bindingCount == 0) {
            throw new BusinessException(403, "Problem does not belong to this contest");
        }

        Long registrationCount = contestRegistrationMapper.selectCount(
                new LambdaQueryWrapper<ContestRegistration>()
                        .eq(ContestRegistration::getContestId, contestId)
                        .eq(ContestRegistration::getUserId, userId)
                        .eq(ContestRegistration::getStatus, "REGISTERED"));
        if (registrationCount == null || registrationCount == 0) {
            throw new BusinessException(403, "Contest registration is required before submitting");
        }
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value;
    }

    private String normalizeLanguage(String language) {
        String normalized = requireText(language, "Language cannot be empty").trim().toUpperCase(Locale.ROOT);
        String result = switch (normalized) {
            case "C++", "CPP", "CPLUSPLUS", "GNU C++" -> "C++";
            case "JAVA" -> "JAVA";
            case "PYTHON", "PY", "PYTHON3" -> "PYTHON";
            case "GO", "GOLANG" -> "GO";
            default -> throw new BusinessException("Unsupported language");
        };
        if (!SUPPORTED_LANGUAGES.contains(result)) {
            throw new BusinessException("Unsupported language");
        }
        return result;
    }

    private void dispatchAfterCommit(Long submissionId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            judgeTaskPublisher.addSubmissionTask(submissionId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                judgeTaskPublisher.addSubmissionTask(submissionId);
            }
        });
    }
}
