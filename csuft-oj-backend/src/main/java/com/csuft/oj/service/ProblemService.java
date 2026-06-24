package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csuft.oj.dto.ProblemCreateRequest;
import com.csuft.oj.dto.ProblemUpdateRequest;
import com.csuft.oj.entity.Problem;
import com.csuft.oj.entity.Submission;
import com.csuft.oj.entity.TestCase;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.mapper.ProblemMapper;
import com.csuft.oj.mapper.SubmissionMapper;
import com.csuft.oj.mapper.TestCaseMapper;
import com.csuft.oj.vo.PageResult;
import com.csuft.oj.vo.ProblemVO;
import com.csuft.oj.vo.TestCaseUploadVO;
import com.csuft.oj.vo.TestCaseVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Problem management and test case upload use cases.
 */
@Service
public class ProblemService {

    private static final int PREVIEW_BYTES = 2048;
    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");
    private static final String USER_STATUS_ACCEPTED = "ACCEPTED";
    private static final String USER_STATUS_WRONG_ANSWER = "WRONG_ANSWER";
    private static final String USER_STATUS_NOT_SUBMITTED = "NOT_SUBMITTED";

    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final TestCaseMapper testCaseMapper;
    private final Path testcaseBasePath;
    private final long maxArchiveBytes;
    private final int maxEntryCount;
    private final long maxEntryBytes;
    private final long maxTotalUncompressedBytes;
    private final int maxCompressionRatio;

    public ProblemService(
            ProblemMapper problemMapper,
            SubmissionMapper submissionMapper,
            TestCaseMapper testCaseMapper,
            @Value("${csuft-oj.upload.testcase-path}") String testcaseBasePath,
            @Value("${csuft-oj.upload.max-archive-bytes:67108864}") long maxArchiveBytes,
            @Value("${csuft-oj.upload.max-entry-count:400}") int maxEntryCount,
            @Value("${csuft-oj.upload.max-entry-bytes:16777216}") long maxEntryBytes,
            @Value("${csuft-oj.upload.max-total-uncompressed-bytes:268435456}") long maxTotalUncompressedBytes,
            @Value("${csuft-oj.upload.max-compression-ratio:100}") int maxCompressionRatio) {
        this.problemMapper = problemMapper;
        this.submissionMapper = submissionMapper;
        this.testCaseMapper = testCaseMapper;
        this.testcaseBasePath = Path.of(testcaseBasePath).toAbsolutePath().normalize();
        this.maxArchiveBytes = maxArchiveBytes;
        this.maxEntryCount = maxEntryCount;
        this.maxEntryBytes = maxEntryBytes;
        this.maxTotalUncompressedBytes = maxTotalUncompressedBytes;
        this.maxCompressionRatio = maxCompressionRatio;
    }

    /**
     * Lists public problems for anonymous/student users and all problems for teachers/admins.
     */
    public PageResult<ProblemVO> listProblems(
            long page,
            long size,
            String keyword,
            Integer visible,
            Long currentUserId,
            boolean canViewHidden) {
        Page<Problem> problemPage = new Page<>(Math.max(page, 1L), Math.min(Math.max(size, 1L), 100L));
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(Problem::getTitle, keyword.trim());
        }
        if (canViewHidden) {
            if (visible != null) {
                wrapper.eq(Problem::getIsVisible, visible);
            }
        } else {
            wrapper.eq(Problem::getIsVisible, 1);
        }
        wrapper.orderByDesc(Problem::getId);

        Page<Problem> result = problemMapper.selectPage(problemPage, wrapper);
        Map<Long, String> userStatuses = userProblemStatuses(
                currentUserId,
                result.getRecords().stream().map(Problem::getId).toList());
        List<ProblemVO> records = result.getRecords().stream()
                .map(problem -> toProblemVO(problem, userStatuses.get(problem.getId())))
                .toList();
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), records);
    }

    /**
     * Returns problem detail while enforcing visibility rules.
     */
    public ProblemVO getProblem(Long id, Long currentUserId, boolean canViewHidden) {
        Problem problem = requireProblem(id);
        if (!canViewHidden && !Integer.valueOf(1).equals(problem.getIsVisible())) {
            throw new BusinessException(403, "Problem is not public");
        }
        return toProblemVO(problem, userProblemStatus(currentUserId, id));
    }

    /**
     * Creates a problem.
     */
    public ProblemVO createProblem(ProblemCreateRequest request, Long authorId) {
        String title = requireText(request.getTitle(), "Problem title cannot be empty");
        String description = requireText(request.getDescription(), "Problem description cannot be empty");
        LocalDateTime now = LocalDateTime.now();

        Problem problem = new Problem();
        problem.setTitle(title);
        problem.setDescription(description);
        problem.setInputDescription(trimToNull(request.getInputDescription()));
        problem.setOutputDescription(trimToNull(request.getOutputDescription()));
        problem.setSampleInput(request.getSampleInput());
        problem.setSampleOutput(request.getSampleOutput());
        problem.setDifficulty(normalizeDifficulty(request.getDifficulty()));
        problem.setTimeLimitMs(defaultPositive(request.getTimeLimitMs(), 1000, "Time limit must be positive"));
        problem.setMemoryLimitKb(defaultPositive(request.getMemoryLimitKb(), 262144, "Memory limit must be positive"));
        problem.setAuthorId(authorId);
        problem.setIsVisible(normalizeVisible(request.getIsVisible()));
        problem.setAcceptedCount(0);
        problem.setSubmitCount(0);
        problem.setCreatedAt(now);
        problem.setUpdatedAt(now);

        problemMapper.insert(problem);
        return toProblemVO(problem, USER_STATUS_NOT_SUBMITTED);
    }

    /**
     * Updates a problem with non-null fields from the request.
     */
    public ProblemVO updateProblem(Long id, ProblemUpdateRequest request) {
        Problem problem = requireProblem(id);

        if (request.getTitle() != null) {
            problem.setTitle(requireText(request.getTitle(), "Problem title cannot be empty"));
        }
        if (request.getDescription() != null) {
            problem.setDescription(requireText(request.getDescription(), "Problem description cannot be empty"));
        }
        if (request.getInputDescription() != null) {
            problem.setInputDescription(trimToNull(request.getInputDescription()));
        }
        if (request.getOutputDescription() != null) {
            problem.setOutputDescription(trimToNull(request.getOutputDescription()));
        }
        if (request.getSampleInput() != null) {
            problem.setSampleInput(request.getSampleInput());
        }
        if (request.getSampleOutput() != null) {
            problem.setSampleOutput(request.getSampleOutput());
        }
        if (request.getDifficulty() != null) {
            problem.setDifficulty(normalizeDifficulty(request.getDifficulty()));
        }
        if (request.getTimeLimitMs() != null) {
            problem.setTimeLimitMs(defaultPositive(request.getTimeLimitMs(), null, "Time limit must be positive"));
        }
        if (request.getMemoryLimitKb() != null) {
            problem.setMemoryLimitKb(defaultPositive(request.getMemoryLimitKb(), null, "Memory limit must be positive"));
        }
        if (request.getIsVisible() != null) {
            problem.setIsVisible(normalizeVisible(request.getIsVisible()));
        }
        problem.setUpdatedAt(LocalDateTime.now());

        problemMapper.updateById(problem);
        return toProblemVO(problem, USER_STATUS_NOT_SUBMITTED);
    }

    /**
     * Deletes a problem by ID.
     */
    public void deleteProblem(Long id) {
        requireProblem(id);
        problemMapper.deleteById(id);
    }

    /**
     * Validates, extracts, and persists uploaded test cases for one problem.
     */
    @Transactional(rollbackFor = Exception.class)
    public TestCaseUploadVO uploadTestCases(Long problemId, MultipartFile file) {
        requireProblem(problemId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded zip file cannot be empty");
        }
        if (file.getSize() > maxArchiveBytes) {
            throw new BusinessException("Test case archive exceeds the maximum allowed size");
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new BusinessException("Only .zip test case archives are supported");
        }

        ZipValidationResult validation = validateZip(file);
        Path problemDir = testcaseBasePath.resolve(String.valueOf(problemId)).normalize();
        ensureInsideBase(problemDir);
        Path stagingDir = testcaseBasePath.resolve(problemId + "_staging_" + UUID.randomUUID()).normalize();
        ensureInsideBase(stagingDir);
        Path backupDir = testcaseBasePath.resolve(problemId + "_backup_" + UUID.randomUUID()).normalize();
        ensureInsideBase(backupDir);

        try {
            recreateDirectory(stagingDir);
            extractZip(file, stagingDir, validation.fileNames());
            replaceProblemDirectory(stagingDir, problemDir, backupDir);
            registerDirectoryRollback(problemDir, backupDir);

            testCaseMapper.delete(new LambdaQueryWrapper<TestCase>().eq(TestCase::getProblemId, problemId));
            List<TestCaseVO> savedCases = saveTestCaseRows(problemId, problemDir, validation);
            return new TestCaseUploadVO(problemId, problemDir.toString(), savedCases.size(), savedCases);
        } catch (IOException ex) {
            throw new BusinessException("Failed to save test case files");
        } finally {
            deleteDirectoryQuietly(stagingDir);
        }
    }

    private ZipValidationResult validateZip(MultipartFile file) {
        Map<String, String> inputFiles = new HashMap<>();
        Map<String, String> outputFiles = new HashMap<>();
        Set<String> fileNames = new HashSet<>();
        long totalUncompressedBytes = 0;
        int entryCount = 0;

        try (InputStream inputStream = file.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                entryCount++;
                if (entryCount > maxEntryCount) {
                    throw new BusinessException("Zip archive contains too many files");
                }

                String name = normalizeZipEntryName(entry.getName());
                if (!fileNames.add(name)) {
                    throw new BusinessException("Duplicate file in zip: " + name);
                }
                if (name.contains("/")) {
                    throw new BusinessException("Test case files must be placed in the zip root directory");
                }

                String lowerName = name.toLowerCase(Locale.ROOT);
                if (lowerName.endsWith(".in")) {
                    inputFiles.put(stem(name), name);
                } else if (lowerName.endsWith(".out")) {
                    outputFiles.put(stem(name), name);
                } else {
                    throw new BusinessException("Unsupported file in zip: " + name);
                }
                totalUncompressedBytes += readEntrySize(zipInputStream, name, totalUncompressedBytes);
                zipInputStream.closeEntry();
            }
        } catch (IOException ex) {
            throw new BusinessException("Invalid zip archive");
        }

        if (inputFiles.isEmpty()) {
            throw new BusinessException("Zip archive must contain at least one .in/.out pair");
        }
        if (!inputFiles.keySet().equals(outputFiles.keySet())) {
            throw new BusinessException("Each .in file must have a matching .out file with the same base name");
        }
        long compressedBytes = Math.max(file.getSize(), 1L);
        if (totalUncompressedBytes > compressedBytes * (long) maxCompressionRatio) {
            throw new BusinessException("Zip archive compression ratio is too high");
        }

        List<String> sortedStems = inputFiles.keySet().stream()
                .sorted(this::compareCaseStem)
                .toList();
        return new ZipValidationResult(inputFiles, outputFiles, fileNames, sortedStems);
    }

    private void extractZip(MultipartFile file, Path targetDir, Set<String> allowedFileNames) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             ZipInputStream zipInputStream = new ZipInputStream(inputStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String name = normalizeZipEntryName(entry.getName());
                if (!allowedFileNames.contains(name)) {
                    throw new BusinessException("Zip content changed during extraction");
                }
                Path targetFile = targetDir.resolve(name).normalize();
                if (!targetFile.startsWith(targetDir)) {
                    throw new BusinessException("Unsafe zip entry path: " + name);
                }
                copyEntryWithLimit(zipInputStream, targetFile, name);
                zipInputStream.closeEntry();
            }
        }
    }

    private List<TestCaseVO> saveTestCaseRows(Long problemId, Path problemDir, ZipValidationResult validation) throws IOException {
        List<TestCaseVO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int order = 1;

        for (String caseStem : validation.sortedStems()) {
            String inputFileName = validation.inputFiles().get(caseStem);
            String outputFileName = validation.outputFiles().get(caseStem);
            Path inputFile = problemDir.resolve(inputFileName).normalize();
            Path outputFile = problemDir.resolve(outputFileName).normalize();
            ensureInsideBase(inputFile);
            ensureInsideBase(outputFile);

            TestCase testCase = new TestCase();
            testCase.setProblemId(problemId);
            testCase.setInputPath(inputFileName);
            testCase.setOutputPath(outputFileName);
            testCase.setInputPreview(readPreview(inputFile));
            testCase.setOutputPreview(readPreview(outputFile));
            testCase.setScore(0);
            testCase.setSortOrder(order);
            testCase.setCreatedAt(now);

            testCaseMapper.insert(testCase);
            result.add(toTestCaseVO(testCase));
            order++;
        }
        return result;
    }

    private Problem requireProblem(Long id) {
        if (id == null) {
            throw new BusinessException("Problem ID cannot be empty");
        }
        Problem problem = problemMapper.selectById(id);
        if (problem == null) {
            throw new BusinessException(404, "Problem not found");
        }
        return problem;
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

    private String normalizeDifficulty(String difficulty) {
        String normalized = StringUtils.hasText(difficulty)
                ? difficulty.trim().toUpperCase(Locale.ROOT)
                : "EASY";
        if (!DIFFICULTIES.contains(normalized)) {
            throw new BusinessException("Difficulty must be EASY, MEDIUM, or HARD");
        }
        return normalized;
    }

    private Integer defaultPositive(Integer value, Integer defaultValue, String message) {
        Integer actual = value == null ? defaultValue : value;
        if (actual == null || actual <= 0) {
            throw new BusinessException(message);
        }
        return actual;
    }

    private Integer normalizeVisible(Integer visible) {
        if (visible == null) {
            return 1;
        }
        if (visible != 0 && visible != 1) {
            throw new BusinessException("Visibility must be 0 or 1");
        }
        return visible;
    }

    private Map<Long, String> userProblemStatuses(Long userId, List<Long> problemIds) {
        Map<Long, String> statuses = new HashMap<>();
        if (userId == null || problemIds == null || problemIds.isEmpty()) {
            return statuses;
        }

        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .select(Submission::getProblemId, Submission::getStatus)
                .eq(Submission::getUserId, userId)
                .in(Submission::getProblemId, problemIds));
        for (Submission submission : submissions) {
            Long problemId = submission.getProblemId();
            if (USER_STATUS_ACCEPTED.equals(statuses.get(problemId))) {
                continue;
            }
            if (USER_STATUS_ACCEPTED.equals(submission.getStatus())) {
                statuses.put(problemId, USER_STATUS_ACCEPTED);
            } else {
                statuses.put(problemId, USER_STATUS_WRONG_ANSWER);
            }
        }
        return statuses;
    }

    private String userProblemStatus(Long userId, Long problemId) {
        return userProblemStatuses(userId, List.of(problemId)).get(problemId);
    }

    private ProblemVO toProblemVO(Problem problem, String submissionStatus) {
        return new ProblemVO(
                problem.getId(),
                problem.getTitle(),
                problem.getDescription(),
                problem.getInputDescription(),
                problem.getOutputDescription(),
                problem.getSampleInput(),
                problem.getSampleOutput(),
                problem.getDifficulty(),
                problem.getTimeLimitMs(),
                problem.getMemoryLimitKb(),
                problem.getAuthorId(),
                problem.getIsVisible(),
                problem.getAcceptedCount(),
                problem.getSubmitCount(),
                submissionStatus == null ? USER_STATUS_NOT_SUBMITTED : submissionStatus,
                problem.getCreatedAt(),
                problem.getUpdatedAt());
    }

    private TestCaseVO toTestCaseVO(TestCase testCase) {
        return new TestCaseVO(
                testCase.getId(),
                testCase.getProblemId(),
                testCase.getInputPath(),
                testCase.getOutputPath(),
                testCase.getInputPreview(),
                testCase.getOutputPreview(),
                testCase.getScore(),
                testCase.getSortOrder(),
                testCase.getCreatedAt());
    }

    private String normalizeZipEntryName(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            throw new BusinessException("Zip entry name cannot be empty");
        }
        String name = rawName.replace('\\', '/');
        Path normalized = Path.of(name).normalize();
        String normalizedName = normalized.toString().replace('\\', '/');
        if (normalized.isAbsolute() || normalizedName.startsWith("../") || normalizedName.equals("..")) {
            throw new BusinessException("Unsafe zip entry path: " + rawName);
        }
        return normalizedName;
    }

    private String stem(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    private int compareCaseStem(String left, String right) {
        Integer leftNumber = parseIntegerOrNull(left);
        Integer rightNumber = parseIntegerOrNull(right);
        if (leftNumber != null && rightNumber != null) {
            return leftNumber.compareTo(rightNumber);
        }
        return left.compareToIgnoreCase(right);
    }

    private Integer parseIntegerOrNull(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String readPreview(Path file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            byte[] bytes = inputStream.readNBytes(PREVIEW_BYTES);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private long readEntrySize(InputStream inputStream, String name, long previousTotal) throws IOException {
        byte[] buffer = new byte[8192];
        long entryBytes = 0;
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            entryBytes += length;
            if (entryBytes > maxEntryBytes) {
                throw new BusinessException("Zip entry is too large: " + name);
            }
            if (previousTotal + entryBytes > maxTotalUncompressedBytes) {
                throw new BusinessException("Zip archive expands beyond the allowed total size");
            }
        }
        return entryBytes;
    }

    private void copyEntryWithLimit(InputStream inputStream, Path targetFile, String name) throws IOException {
        byte[] buffer = new byte[8192];
        long copied = 0;
        try (var outputStream = Files.newOutputStream(targetFile)) {
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                copied += length;
                if (copied > maxEntryBytes) {
                    throw new BusinessException("Zip entry is too large: " + name);
                }
                outputStream.write(buffer, 0, length);
            }
        }
    }

    private void replaceProblemDirectory(Path stagingDir, Path problemDir, Path backupDir) throws IOException {
        boolean oldDirectoryMoved = false;
        try {
            if (Files.exists(problemDir)) {
                moveDirectory(problemDir, backupDir);
                oldDirectoryMoved = true;
            }
            moveDirectory(stagingDir, problemDir);
        } catch (IOException ex) {
            deleteDirectoryQuietly(problemDir);
            if (oldDirectoryMoved && Files.exists(backupDir)) {
                moveDirectory(backupDir, problemDir);
            }
            throw ex;
        }
    }

    private void registerDirectoryRollback(Path problemDir, Path backupDir) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteDirectoryQuietly(backupDir);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    deleteDirectoryQuietly(backupDir);
                    return;
                }
                deleteDirectoryQuietly(problemDir);
                if (Files.exists(backupDir)) {
                    try {
                        moveDirectory(backupDir, problemDir);
                    } catch (IOException ex) {
                        throw new IllegalStateException("Failed to restore previous test case directory", ex);
                    }
                }
            }
        });
    }

    private void moveDirectory(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(source, target);
        }
    }

    private void recreateDirectory(Path directory) throws IOException {
        deleteDirectoryQuietly(directory);
        Files.createDirectories(directory);
    }

    private void deleteDirectoryQuietly(Path directory) {
        if (!Files.exists(directory)) {
            return;
        }
        try (var paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best effort cleanup; the main transaction still reports persistence failures.
                        }
                    });
        } catch (IOException ignored) {
            // Best effort cleanup only.
        }
    }

    private void ensureInsideBase(Path path) {
        if (!path.normalize().startsWith(testcaseBasePath)) {
            throw new BusinessException("Resolved test case path is outside configured upload directory");
        }
    }

    private record ZipValidationResult(
            Map<String, String> inputFiles,
            Map<String, String> outputFiles,
            Set<String> fileNames,
            List<String> sortedStems) {
    }
}
