package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csuft.oj.dto.ContestCreateRequest;
import com.csuft.oj.dto.ContestProblemBatchBindRequest;
import com.csuft.oj.dto.ContestProblemBindRequest;
import com.csuft.oj.dto.ContestUpdateRequest;
import com.csuft.oj.entity.Contest;
import com.csuft.oj.entity.ContestProblem;
import com.csuft.oj.entity.ContestRegistration;
import com.csuft.oj.entity.Problem;
import com.csuft.oj.entity.Submission;
import com.csuft.oj.entity.User;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.mapper.ContestMapper;
import com.csuft.oj.mapper.ContestProblemMapper;
import com.csuft.oj.mapper.ContestRegistrationMapper;
import com.csuft.oj.mapper.ProblemMapper;
import com.csuft.oj.mapper.SubmissionMapper;
import com.csuft.oj.mapper.UserMapper;
import com.csuft.oj.vo.ContestProblemVO;
import com.csuft.oj.vo.ContestRankProblemVO;
import com.csuft.oj.vo.ContestRankRowVO;
import com.csuft.oj.vo.ContestRanklistVO;
import com.csuft.oj.vo.ContestRegistrationVO;
import com.csuft.oj.vo.ContestVO;
import com.csuft.oj.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Contest management, registration, and live ranklist use cases.
 */
@Service
public class ContestService {

    private static final Set<String> RULE_TYPES = Set.of("ACM", "IOI");

    private final ContestMapper contestMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ContestRegistrationMapper contestRegistrationMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final UserMapper userMapper;

    public ContestService(
            ContestMapper contestMapper,
            ContestProblemMapper contestProblemMapper,
            ContestRegistrationMapper contestRegistrationMapper,
            ProblemMapper problemMapper,
            SubmissionMapper submissionMapper,
            UserMapper userMapper) {
        this.contestMapper = contestMapper;
        this.contestProblemMapper = contestProblemMapper;
        this.contestRegistrationMapper = contestRegistrationMapper;
        this.problemMapper = problemMapper;
        this.submissionMapper = submissionMapper;
        this.userMapper = userMapper;
    }

    /**
     * Lists contests. Non-privileged users only see public, enabled contests.
     */
    public PageResult<ContestVO> listContests(long page, long size, boolean canViewAll) {
        Page<Contest> contestPage = new Page<>(Math.max(page, 1L), Math.min(Math.max(size, 1L), 100L));
        LambdaQueryWrapper<Contest> wrapper = new LambdaQueryWrapper<>();
        if (!canViewAll) {
            wrapper.eq(Contest::getIsPublic, 1).eq(Contest::getStatus, 1);
        }
        wrapper.orderByDesc(Contest::getStartTime).orderByDesc(Contest::getId);
        Page<Contest> result = contestMapper.selectPage(contestPage, wrapper);
        return new PageResult<>(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                result.getRecords().stream().map(this::toContestVO).toList());
    }

    /**
     * Gets contest detail with visibility checks.
     */
    public ContestVO getContest(Long contestId, boolean canViewAll) {
        Contest contest = requireContest(contestId);
        ensureContestVisible(contest, canViewAll);
        return toContestVO(contest);
    }

    /**
     * Creates a contest.
     */
    public ContestVO createContest(ContestCreateRequest request, Long createdBy) {
        Contest contest = new Contest();
        LocalDateTime now = LocalDateTime.now();
        contest.setTitle(requireText(request.getTitle(), "Contest title cannot be empty"));
        contest.setDescription(trimToNull(request.getDescription()));
        contest.setRuleType(normalizeRuleType(request.getRuleType()));
        contest.setStartTime(requireTime(request.getStartTime(), "Contest start time cannot be empty"));
        contest.setEndTime(requireTime(request.getEndTime(), "Contest end time cannot be empty"));
        validateTimeRange(contest.getStartTime(), contest.getEndTime());
        contest.setIsPublic(normalizeFlag(request.getIsPublic(), 1, "isPublic must be 0 or 1"));
        contest.setStatus(1);
        contest.setCreatedBy(createdBy);
        contest.setCreatedAt(now);
        contest.setUpdatedAt(now);
        contestMapper.insert(contest);
        return toContestVO(contest);
    }

    /**
     * Updates a contest.
     */
    public ContestVO updateContest(Long contestId, ContestUpdateRequest request) {
        Contest contest = requireContest(contestId);
        if (request.getTitle() != null) {
            contest.setTitle(requireText(request.getTitle(), "Contest title cannot be empty"));
        }
        if (request.getDescription() != null) {
            contest.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getRuleType() != null) {
            contest.setRuleType(normalizeRuleType(request.getRuleType()));
        }
        if (request.getStartTime() != null) {
            contest.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            contest.setEndTime(request.getEndTime());
        }
        validateTimeRange(contest.getStartTime(), contest.getEndTime());
        if (request.getIsPublic() != null) {
            contest.setIsPublic(normalizeFlag(request.getIsPublic(), 1, "isPublic must be 0 or 1"));
        }
        if (request.getStatus() != null) {
            contest.setStatus(normalizeFlag(request.getStatus(), 1, "status must be 0 or 1"));
        }
        contest.setUpdatedAt(LocalDateTime.now());
        contestMapper.updateById(contest);
        return toContestVO(contest);
    }

    /**
     * Deletes a contest and its related bindings and registrations.
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteContest(Long contestId) {
        requireContest(contestId);
        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>().eq(ContestProblem::getContestId, contestId));
        contestRegistrationMapper.delete(new LambdaQueryWrapper<ContestRegistration>().eq(ContestRegistration::getContestId, contestId));
        contestMapper.deleteById(contestId);
    }

    /**
     * Replaces all problem bindings of a contest.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<ContestProblemVO> bindProblems(Long contestId, ContestProblemBatchBindRequest request) {
        requireContest(contestId);
        if (request == null || request.getProblems() == null || request.getProblems().isEmpty()) {
            throw new BusinessException("Contest problems cannot be empty");
        }

        validateProblemBindings(request.getProblems());
        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>().eq(ContestProblem::getContestId, contestId));

        LocalDateTime now = LocalDateTime.now();
        List<ContestProblemVO> result = new ArrayList<>();
        for (ContestProblemBindRequest item : request.getProblems()) {
            ContestProblem contestProblem = new ContestProblem();
            contestProblem.setContestId(contestId);
            contestProblem.setProblemId(item.getProblemId());
            contestProblem.setAlias(item.getAlias().trim().toUpperCase(Locale.ROOT));
            contestProblem.setSortOrder(item.getSortOrder());
            contestProblem.setScore(item.getScore() == null ? 100 : item.getScore());
            contestProblem.setCreatedAt(now);
            contestProblemMapper.insert(contestProblem);
            result.add(toContestProblemVO(contestProblem));
        }
        return result;
    }

    /**
     * Lists contest problem bindings.
     */
    public List<ContestProblemVO> listContestProblems(Long contestId, boolean canViewAll) {
        Contest contest = requireContest(contestId);
        ensureContestVisible(contest, canViewAll);
        return loadContestProblems(contestId).stream().map(this::toContestProblemVO).toList();
    }

    /**
     * Registers a user for a contest.
     */
    public ContestRegistrationVO register(Long contestId, Long userId, boolean canViewAll) {
        if (userId == null) {
            throw new BusinessException(401, "Login required");
        }
        Contest contest = requireContest(contestId);
        ensureContestVisible(contest, canViewAll);
        if (!Integer.valueOf(1).equals(contest.getStatus())) {
            throw new BusinessException("Contest is disabled");
        }

        ContestRegistration existing = contestRegistrationMapper.selectOne(new LambdaQueryWrapper<ContestRegistration>()
                .eq(ContestRegistration::getContestId, contestId)
                .eq(ContestRegistration::getUserId, userId)
                .last("LIMIT 1"));
        if (existing != null) {
            if ("REGISTERED".equals(existing.getStatus())) {
                return toContestRegistrationVO(existing);
            }
            existing.setStatus("REGISTERED");
            existing.setRegisteredAt(LocalDateTime.now());
            contestRegistrationMapper.updateById(existing);
            return toContestRegistrationVO(existing);
        }

        ContestRegistration registration = new ContestRegistration();
        registration.setContestId(contestId);
        registration.setUserId(userId);
        registration.setStatus("REGISTERED");
        registration.setRegisteredAt(LocalDateTime.now());
        contestRegistrationMapper.insert(registration);
        return toContestRegistrationVO(registration);
    }

    /**
     * Calculates live contest ranklist from registrations and submissions.
     */
    public ContestRanklistVO ranklist(Long contestId, boolean canViewAll) {
        Contest contest = requireContest(contestId);
        ensureContestVisible(contest, canViewAll);
        List<ContestProblem> contestProblems = loadContestProblems(contestId);
        List<ContestRegistration> registrations = loadRegistrations(contestId);
        if (registrations.isEmpty()) {
            return new ContestRanklistVO(contestId, contest.getRuleType(), contestProblems.stream().map(this::toContestProblemVO).toList(), List.of());
        }

        List<Long> userIds = registrations.stream().map(ContestRegistration::getUserId).distinct().toList();
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        Map<Long, ContestProblem> contestProblemByProblemId = contestProblems.stream()
                .collect(Collectors.toMap(ContestProblem::getProblemId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        List<Submission> submissions = loadContestSubmissions(contest, userIds, contestProblemByProblemId.keySet());
        Map<Long, List<Submission>> submissionsByUser = submissions.stream()
                .collect(Collectors.groupingBy(Submission::getUserId));

        List<ContestRankRowVO> rows = "IOI".equals(contest.getRuleType())
                ? buildIoiRows(contestProblems, registrations, users, submissionsByUser)
                : buildAcmRows(contest, contestProblems, registrations, users, submissionsByUser);
        assignRanks(rows, "IOI".equals(contest.getRuleType()));

        return new ContestRanklistVO(
                contestId,
                contest.getRuleType(),
                contestProblems.stream().map(this::toContestProblemVO).toList(),
                rows);
    }

    private List<ContestRankRowVO> buildAcmRows(
            Contest contest,
            List<ContestProblem> contestProblems,
            List<ContestRegistration> registrations,
            Map<Long, User> users,
            Map<Long, List<Submission>> submissionsByUser) {
        List<ContestRankRowVO> rows = new ArrayList<>();
        for (ContestRegistration registration : registrations) {
            Long userId = registration.getUserId();
            Map<Long, List<Submission>> submissionsByProblem = submissionsByUser
                    .getOrDefault(userId, List.of())
                    .stream()
                    .collect(Collectors.groupingBy(Submission::getProblemId));

            int acceptedCount = 0;
            long totalPenalty = 0;
            List<ContestRankProblemVO> problemDetails = new ArrayList<>();
            for (ContestProblem contestProblem : contestProblems) {
                AcmProblemResult result = calculateAcmProblem(contest, submissionsByProblem.getOrDefault(contestProblem.getProblemId(), List.of()));
                if (result.accepted()) {
                    acceptedCount++;
                    totalPenalty += result.penaltyMinutes();
                }
                problemDetails.add(new ContestRankProblemVO(
                        contestProblem.getAlias(),
                        contestProblem.getProblemId(),
                        result.accepted(),
                        result.wrongAttemptsBeforeAc(),
                        result.accepted() ? result.penaltyMinutes() : null,
                        0,
                        result.acceptedAt()));
            }
            User user = users.get(userId);
            rows.add(new ContestRankRowVO(0L, userId, username(user), nickname(user), acceptedCount, totalPenalty, 0, problemDetails));
        }
        rows.sort(Comparator
                .comparing(ContestRankRowVO::getAcceptedCount, Comparator.reverseOrder())
                .thenComparing(ContestRankRowVO::getTotalPenaltyMinutes)
                .thenComparing(ContestRankRowVO::getUserId));
        return rows;
    }

    private AcmProblemResult calculateAcmProblem(Contest contest, List<Submission> submissions) {
        List<Submission> ordered = submissions.stream()
                .sorted(Comparator.comparing(Submission::getCreatedAt).thenComparing(Submission::getId))
                .toList();
        int wrongBeforeAc = 0;
        for (Submission submission : ordered) {
            if ("ACCEPTED".equals(submission.getStatus())) {
                long elapsedMinutes = Math.max(0, Duration.between(contest.getStartTime(), submission.getCreatedAt()).toMinutes());
                return new AcmProblemResult(true, wrongBeforeAc, elapsedMinutes + wrongBeforeAc * 20L, submission.getCreatedAt());
            }
            if (isAcmPenaltyStatus(submission.getStatus())) {
                wrongBeforeAc++;
            }
        }
        return new AcmProblemResult(false, wrongBeforeAc, 0L, null);
    }

    private List<ContestRankRowVO> buildIoiRows(
            List<ContestProblem> contestProblems,
            List<ContestRegistration> registrations,
            Map<Long, User> users,
            Map<Long, List<Submission>> submissionsByUser) {
        List<ContestRankRowVO> rows = new ArrayList<>();
        for (ContestRegistration registration : registrations) {
            Long userId = registration.getUserId();
            Map<Long, List<Submission>> submissionsByProblem = submissionsByUser
                    .getOrDefault(userId, List.of())
                    .stream()
                    .collect(Collectors.groupingBy(Submission::getProblemId));

            int totalScore = 0;
            List<ContestRankProblemVO> problemDetails = new ArrayList<>();
            for (ContestProblem contestProblem : contestProblems) {
                IoiProblemResult result = calculateIoiProblem(contestProblem, submissionsByProblem.getOrDefault(contestProblem.getProblemId(), List.of()));
                totalScore += result.score();
                problemDetails.add(new ContestRankProblemVO(
                        contestProblem.getAlias(),
                        contestProblem.getProblemId(),
                        result.score() >= fullScore(contestProblem),
                        0,
                        null,
                        result.score(),
                        result.bestAt()));
            }
            User user = users.get(userId);
            rows.add(new ContestRankRowVO(0L, userId, username(user), nickname(user), 0, 0L, totalScore, problemDetails));
        }
        rows.sort(Comparator
                .comparing(ContestRankRowVO::getTotalScore, Comparator.reverseOrder())
                .thenComparing(ContestRankRowVO::getUserId));
        return rows;
    }

    private IoiProblemResult calculateIoiProblem(ContestProblem contestProblem, List<Submission> submissions) {
        int fullScore = fullScore(contestProblem);
        int bestScore = 0;
        LocalDateTime bestAt = null;
        for (Submission submission : submissions) {
            int normalizedSubmissionScore = Math.max(0, Math.min(100, submission.getScore() == null ? 0 : submission.getScore()));
            int weightedScore = fullScore * normalizedSubmissionScore / 100;
            if (weightedScore > bestScore) {
                bestScore = weightedScore;
                bestAt = submission.getCreatedAt();
            }
        }
        return new IoiProblemResult(bestScore, bestAt);
    }

    private void assignRanks(List<ContestRankRowVO> rows, boolean ioi) {
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRank((long) i + 1);
        }
    }

    private boolean isAcmPenaltyStatus(String status) {
        return status != null
                && !"COMPILE_ERROR".equals(status)
                && !"PENDING".equals(status)
                && !"JUDGING".equals(status)
                && !"ACCEPTED".equals(status);
    }

    private int fullScore(ContestProblem contestProblem) {
        return contestProblem.getScore() == null ? 100 : contestProblem.getScore();
    }

    private List<Submission> loadContestSubmissions(Contest contest, List<Long> userIds, Set<Long> problemIds) {
        if (userIds.isEmpty() || problemIds.isEmpty()) {
            return List.of();
        }
        return submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getContestId, contest.getId())
                .in(Submission::getUserId, userIds)
                .in(Submission::getProblemId, problemIds)
                .ge(Submission::getCreatedAt, contest.getStartTime())
                .le(Submission::getCreatedAt, contest.getEndTime())
                .orderByAsc(Submission::getCreatedAt)
                .orderByAsc(Submission::getId));
    }

    private List<ContestProblem> loadContestProblems(Long contestId) {
        return contestProblemMapper.selectList(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId)
                .orderByAsc(ContestProblem::getSortOrder)
                .orderByAsc(ContestProblem::getId));
    }

    private List<ContestRegistration> loadRegistrations(Long contestId) {
        return contestRegistrationMapper.selectList(new LambdaQueryWrapper<ContestRegistration>()
                .eq(ContestRegistration::getContestId, contestId)
                .eq(ContestRegistration::getStatus, "REGISTERED")
                .orderByAsc(ContestRegistration::getRegisteredAt)
                .orderByAsc(ContestRegistration::getId));
    }

    private void validateProblemBindings(List<ContestProblemBindRequest> problems) {
        Set<Long> problemIds = new HashSet<>();
        Set<String> aliases = new HashSet<>();
        for (ContestProblemBindRequest item : problems) {
            if (item.getProblemId() == null) {
                throw new BusinessException("Problem ID cannot be empty");
            }
            if (problemMapper.selectById(item.getProblemId()) == null) {
                throw new BusinessException(404, "Problem not found: " + item.getProblemId());
            }
            if (!problemIds.add(item.getProblemId())) {
                throw new BusinessException("Duplicate problem ID: " + item.getProblemId());
            }
            String alias = requireText(item.getAlias(), "Problem alias cannot be empty").toUpperCase(Locale.ROOT);
            if (!aliases.add(alias)) {
                throw new BusinessException("Duplicate problem alias: " + alias);
            }
            if (item.getSortOrder() == null || item.getSortOrder() < 0) {
                throw new BusinessException("Problem sort order must be non-negative");
            }
            if (item.getScore() != null && item.getScore() <= 0) {
                throw new BusinessException("Problem score must be positive");
            }
        }
    }

    private Contest requireContest(Long contestId) {
        if (contestId == null) {
            throw new BusinessException("Contest ID cannot be empty");
        }
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(404, "Contest not found");
        }
        return contest;
    }

    private void ensureContestVisible(Contest contest, boolean canViewAll) {
        if (!canViewAll && (!Integer.valueOf(1).equals(contest.getIsPublic()) || !Integer.valueOf(1).equals(contest.getStatus()))) {
            throw new BusinessException(403, "Contest is not public");
        }
    }

    private String normalizeRuleType(String ruleType) {
        String normalized = StringUtils.hasText(ruleType) ? ruleType.trim().toUpperCase(Locale.ROOT) : "ACM";
        if (!RULE_TYPES.contains(normalized)) {
            throw new BusinessException("Rule type must be ACM or IOI");
        }
        return normalized;
    }

    private Integer normalizeFlag(Integer value, Integer defaultValue, String message) {
        Integer actual = value == null ? defaultValue : value;
        if (actual == null || (actual != 0 && actual != 1)) {
            throw new BusinessException(message);
        }
        return actual;
    }

    private LocalDateTime requireTime(LocalDateTime value, String message) {
        if (value == null) {
            throw new BusinessException(message);
        }
        return value;
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new BusinessException("Contest end time must be after start time");
        }
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String username(User user) {
        return user == null ? null : user.getUsername();
    }

    private String nickname(User user) {
        return user == null ? null : user.getNickname();
    }

    private ContestVO toContestVO(Contest contest) {
        return new ContestVO(
                contest.getId(),
                contest.getTitle(),
                contest.getDescription(),
                contest.getRuleType(),
                contest.getStartTime(),
                contest.getEndTime(),
                contest.getIsPublic(),
                contest.getStatus(),
                contest.getCreatedBy(),
                contest.getCreatedAt(),
                contest.getUpdatedAt());
    }

    private ContestProblemVO toContestProblemVO(ContestProblem contestProblem) {
        return new ContestProblemVO(
                contestProblem.getId(),
                contestProblem.getContestId(),
                contestProblem.getProblemId(),
                contestProblem.getAlias(),
                contestProblem.getSortOrder(),
                contestProblem.getScore(),
                contestProblem.getCreatedAt());
    }

    private ContestRegistrationVO toContestRegistrationVO(ContestRegistration registration) {
        return new ContestRegistrationVO(
                registration.getId(),
                registration.getContestId(),
                registration.getUserId(),
                registration.getStatus(),
                registration.getRegisteredAt());
    }

    private record AcmProblemResult(boolean accepted, int wrongAttemptsBeforeAc, long penaltyMinutes, LocalDateTime acceptedAt) {
    }

    private record IoiProblemResult(int score, LocalDateTime bestAt) {
    }
}
