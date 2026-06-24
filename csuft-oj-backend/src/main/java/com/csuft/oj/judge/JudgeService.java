package com.csuft.oj.judge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csuft.oj.entity.Problem;
import com.csuft.oj.entity.Submission;
import com.csuft.oj.entity.TestCase;
import com.csuft.oj.mapper.JudgeStatisticsMapper;
import com.csuft.oj.mapper.ProblemMapper;
import com.csuft.oj.mapper.SubmissionMapper;
import com.csuft.oj.mapper.TestCaseMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Executes the asynchronous judging pipeline for one submission.
 */
@Slf4j
@Service
public class JudgeService {

    private static final int COMPILE_TIMEOUT_SECONDS = 30;

    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final TestCaseMapper testCaseMapper;
    private final JudgeStatisticsMapper judgeStatisticsMapper;
    private final TransactionTemplate transactionTemplate;
    private final MeterRegistry meterRegistry;
    private final Path testcaseBasePath;
    private final Path judgeTempBasePath;
    private final int maxOutputBytes;
    private final String executionMode;
    private final String sandboxCommand;
    private final int compileMemoryLimitKb;

    public JudgeService(
            SubmissionMapper submissionMapper,
            ProblemMapper problemMapper,
            TestCaseMapper testCaseMapper,
            JudgeStatisticsMapper judgeStatisticsMapper,
            TransactionTemplate transactionTemplate,
            MeterRegistry meterRegistry,
            @Value("${csuft-oj.upload.testcase-path}") String testcaseBasePath,
            @Value("${csuft-oj.judge.temp-path}") String judgeTempBasePath,
            @Value("${csuft-oj.judge.max-output-bytes:8388608}") int maxOutputBytes,
            @Value("${csuft-oj.judge.execution-mode:HOST}") String executionMode,
            @Value("${csuft-oj.judge.sandbox-command:}") String sandboxCommand,
            @Value("${csuft-oj.judge.compile-memory-limit-kb:1048576}") int compileMemoryLimitKb) {
        this.submissionMapper = submissionMapper;
        this.problemMapper = problemMapper;
        this.testCaseMapper = testCaseMapper;
        this.judgeStatisticsMapper = judgeStatisticsMapper;
        this.transactionTemplate = transactionTemplate;
        this.meterRegistry = meterRegistry;
        this.testcaseBasePath = Path.of(testcaseBasePath).toAbsolutePath().normalize();
        this.judgeTempBasePath = Path.of(judgeTempBasePath).toAbsolutePath().normalize();
        this.maxOutputBytes = maxOutputBytes;
        this.executionMode = executionMode;
        this.sandboxCommand = sandboxCommand;
        this.compileMemoryLimitKb = compileMemoryLimitKb;
    }

    /**
     * Runs compile, execute, compare, and result archive steps for a submission.
     */
    public void judge(Long submissionId) {
        Path workDir = judgeTempBasePath.resolve(String.valueOf(submissionId)).normalize();
        Timer.Sample timerSample = Timer.start(meterRegistry);
        try {
            markJudging(submissionId);

            Submission submission = submissionMapper.selectById(submissionId);
            if (submission == null) {
                log.warn("Submission {} does not exist, skip judge task", submissionId);
                return;
            }

            Problem problem = problemMapper.selectById(submission.getProblemId());
            if (problem == null) {
                finish(submission, "SYSTEM_ERROR", "Problem not found", "Problem not found", 0, 0, 0);
                return;
            }

            List<TestCase> testCases = loadTestCases(problem.getId());
            if (testCases.isEmpty()) {
                finish(submission, "SYSTEM_ERROR", "No test cases found", "No test cases found", 0, 0, 0);
                return;
            }

            recreateDirectory(workDir);
            JudgeProgram program = prepareProgram(submission, workDir);
            CompileResult compileResult = compile(program, workDir);
            if (!compileResult.success()) {
                finish(submission, "COMPILE_ERROR", compileResult.errorLog(), compileResult.errorLog(), 0, 0, 0);
                return;
            }

            JudgeRunSummary summary = runAllTestCases(problem, program, testCases);
            archiveResult(submission, problem, summary);
        } catch (Exception ex) {
            log.error("Unexpected judge error for submission {}", submissionId, ex);
            Submission submission = submissionMapper.selectById(submissionId);
            if (submission != null) {
                finish(submission, "SYSTEM_ERROR", "Judge system error", ex.getMessage(), 0, 0, 0);
            }
        } finally {
            deleteDirectoryQuietly(workDir);
            timerSample.stop(meterRegistry.timer("csuft_oj_judge_duration"));
        }
    }

    private JudgeRunSummary runAllTestCases(
            Problem problem,
            JudgeProgram program,
            List<TestCase> testCases) throws Exception {
        int maxTimeMs = 0;
        int maxMemoryKb = 0;

        for (TestCase testCase : testCases) {
            Path inputFile = testcasePath(problem.getId(), testCase.getInputPath());
            Path expectedFile = testcasePath(problem.getId(), testCase.getOutputPath());
            RunResult runResult = runProgram(
                    program,
                    inputFile,
                    problem.getTimeLimitMs(),
                    problem.getMemoryLimitKb());
            maxTimeMs = Math.max(maxTimeMs, runResult.timeUsedMs());

            if (runResult.timeout()) {
                return new JudgeRunSummary("TIME_LIMIT_EXCEEDED",
                        "Time limit exceeded on test case " + testCase.getSortOrder(),
                        null,
                        maxTimeMs,
                        maxMemoryKb,
                        0);
            }
            if (runResult.exitCode() != 0) {
                String message = "Runtime error on test case " + testCase.getSortOrder();
                return new JudgeRunSummary("RUNTIME_ERROR",
                        message,
                        runResult.stderr(),
                        maxTimeMs,
                        maxMemoryKb,
                        0);
            }

            byte[] expectedOutput = Files.readAllBytes(expectedFile);
            if (!outputEquals(runResult.stdout(), expectedOutput)) {
                String message = "Wrong answer on test case " + testCase.getSortOrder();
                return new JudgeRunSummary("WRONG_ANSWER", message, null, maxTimeMs, maxMemoryKb, 0);
            }
        }

        return new JudgeRunSummary("ACCEPTED", "Accepted", null, maxTimeMs, maxMemoryKb, 100);
    }

    private List<TestCase> loadTestCases(Long problemId) {
        return testCaseMapper.selectList(new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, problemId)
                .orderByAsc(TestCase::getSortOrder)
                .orderByAsc(TestCase::getId));
    }

    private JudgeProgram prepareProgram(Submission submission, Path workDir) throws IOException {
        Language language = Language.from(submission.getLanguage());
        Path sourceFile = workDir.resolve(language.sourceFileName()).normalize();
        Files.writeString(sourceFile, submission.getCode(), StandardCharsets.UTF_8);
        return new JudgeProgram(language, sourceFile, workDir);
    }

    private CompileResult compile(JudgeProgram program, Path workDir) throws Exception {
        List<String> command = program.language().compileCommand(program, workDir);
        if (command.isEmpty()) {
            return new CompileResult(true, null);
        }

        ProcessBuilder processBuilder = processBuilder(command, workDir, compileMemoryLimitKb);
        processBuilder.directory(workDir.toFile());
        Process process = processBuilder.start();
        CompletableFuture<byte[]> stderrFuture = readLimitedAsync(process.getErrorStream());
        CompletableFuture<byte[]> stdoutFuture = readLimitedAsync(process.getInputStream());
        boolean finished = process.waitFor(COMPILE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!finished) {
            terminateProcessTree(process);
            return new CompileResult(false, "Compilation timed out");
        }

        byte[] stderr = stderrFuture.get(1, TimeUnit.SECONDS);
        byte[] stdout = stdoutFuture.get(1, TimeUnit.SECONDS);
        if (process.exitValue() != 0) {
            return new CompileResult(false, truncateLog(stderr.length > 0 ? stderr : stdout));
        }
        return new CompileResult(true, null);
    }

    private RunResult runProgram(
            JudgeProgram program,
            Path inputFile,
            Integer timeLimitMs,
            Integer memoryLimitKb) throws Exception {
        int limitMs = timeLimitMs == null || timeLimitMs <= 0 ? 1000 : timeLimitMs;
        ProcessBuilder processBuilder = processBuilder(
                program.language().runCommand(program),
                program.workDir(),
                memoryLimitKb == null || memoryLimitKb <= 0 ? 262144 : memoryLimitKb);
        processBuilder.directory(program.workDir().toFile());
        processBuilder.redirectInput(inputFile.toFile());

        long startedAt = System.nanoTime();
        Process process = processBuilder.start();
        CompletableFuture<byte[]> stdoutFuture = readLimitedAsync(process.getInputStream());
        CompletableFuture<byte[]> stderrFuture = readLimitedAsync(process.getErrorStream());
        boolean finished = process.waitFor(limitMs, TimeUnit.MILLISECONDS);
        int elapsedMs = (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        if (!finished) {
            terminateProcessTree(process);
            return new RunResult(true, -1, new byte[0], "Time limit exceeded", elapsedMs);
        }

        byte[] stdout = stdoutFuture.get(1, TimeUnit.SECONDS);
        byte[] stderr = stderrFuture.get(1, TimeUnit.SECONDS);
        return new RunResult(false, process.exitValue(), stdout, truncateLog(stderr), elapsedMs);
    }

    private ProcessBuilder processBuilder(List<String> command, Path workDir, int memoryLimitKb) {
        if (!"SANDBOX".equalsIgnoreCase(executionMode)) {
            return new ProcessBuilder(command);
        }
        if (!StringUtils.hasText(sandboxCommand)) {
            throw new IllegalStateException("Sandbox execution requires csuft-oj.judge.sandbox-command");
        }
        List<String> sandboxedCommand = new ArrayList<>();
        sandboxedCommand.add(sandboxCommand);
        sandboxedCommand.add("--work-dir");
        sandboxedCommand.add(workDir.toString());
        sandboxedCommand.add("--memory-kb");
        sandboxedCommand.add(String.valueOf(memoryLimitKb));
        sandboxedCommand.add("--");
        sandboxedCommand.addAll(command);
        return new ProcessBuilder(sandboxedCommand);
    }

    private void terminateProcessTree(Process process) {
        process.descendants().forEach(handle -> {
            try {
                handle.destroyForcibly();
            } catch (Exception ignored) {
                // Best effort termination of user-created child processes.
            }
        });
        process.destroyForcibly();
    }

    private boolean outputEquals(byte[] actualBytes, byte[] expectedBytes) {
        return normalizeOutput(actualBytes).equals(normalizeOutput(expectedBytes));
    }

    private String normalizeOutput(byte[] bytes) {
        String value = new String(bytes, StandardCharsets.UTF_8)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        value = value.replaceAll("[ \\t]+", " ");
        return stripTrailingNewLines(value);
    }

    private String stripTrailingNewLines(String value) {
        int end = value.length();
        while (end > 0) {
            char ch = value.charAt(end - 1);
            if (ch == '\n' || ch == ' ') {
                end--;
            } else {
                break;
            }
        }
        return value.substring(0, end);
    }

    private CompletableFuture<byte[]> readLimitedAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream stream = inputStream;
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int total = 0;
                int length;
                while ((length = stream.read(buffer)) != -1) {
                    total += length;
                    if (total > maxOutputBytes) {
                        int allowed = Math.max(0, length - (total - maxOutputBytes));
                        outputStream.write(buffer, 0, allowed);
                        break;
                    }
                    outputStream.write(buffer, 0, length);
                }
                return outputStream.toByteArray();
            } catch (IOException ex) {
                return ("Failed to read process output: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8);
            }
        });
    }

    private Path testcasePath(Long problemId, String fileName) {
        Path path = testcaseBasePath.resolve(String.valueOf(problemId)).resolve(fileName).normalize();
        if (!path.startsWith(testcaseBasePath)) {
            throw new IllegalArgumentException("Unsafe test case path: " + fileName);
        }
        return path;
    }

    private void markJudging(Long submissionId) {
        Submission update = new Submission();
        update.setId(submissionId);
        update.setStatus("JUDGING");
        update.setJudgeMessage("Judging");
        update.setErrorLog(null);
        update.setJudgedAt(null);
        submissionMapper.updateById(update);
    }

    private void archiveResult(Submission submission, Problem problem, JudgeRunSummary summary) {
        if (!"ACCEPTED".equals(summary.status())) {
            finish(submission,
                    summary.status(),
                    summary.message(),
                    summary.errorLog(),
                    summary.maxTimeMs(),
                    summary.maxMemoryKb(),
                    summary.score());
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            finish(submission,
                    summary.status(),
                    summary.message(),
                    summary.errorLog(),
                    summary.maxTimeMs(),
                    summary.maxMemoryKb(),
                    summary.score());
            recordFirstAcceptedSolve(submission, problem);
        });
    }

    private void finish(
            Submission submission,
            String status,
            String judgeMessage,
            String errorLog,
            int maxTimeMs,
            int maxMemoryKb,
            int score) {
        Submission update = new Submission();
        update.setId(submission.getId());
        update.setStatus(status);
        update.setJudgeMessage(judgeMessage);
        update.setErrorLog(errorLog);
        update.setTimeUsedMs(maxTimeMs);
        update.setMemoryUsedKb(maxMemoryKb);
        update.setScore(score);
        update.setJudgedAt(LocalDateTime.now());
        submissionMapper.updateById(update);
        meterRegistry.counter("csuft_oj_judge_results_total", "status", status).increment();
    }

    private void recordFirstAcceptedSolve(Submission submission, Problem problem) {
        int inserted = judgeStatisticsMapper.insertSolvedRecord(
                submission.getUserId(),
                problem.getId(),
                submission.getId(),
                LocalDateTime.now());
        if (inserted == 0) {
            return;
        }
        judgeStatisticsMapper.incrementUserSolvedCount(submission.getUserId());
        judgeStatisticsMapper.incrementProblemSolvedCount(problem.getId());
    }

    private String truncateLog(byte[] bytes) {
        return truncateLog(new String(bytes, StandardCharsets.UTF_8));
    }

    private String truncateLog(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        int maxLength = 4096;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
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
                            // Best effort cleanup for judge temp files.
                        }
                    });
        } catch (IOException ignored) {
            // Best effort cleanup only.
        }
    }

    private enum Language {
        CPP("Main.cpp") {
            @Override
            List<String> compileCommand(JudgeProgram program, Path workDir) {
                return List.of("g++", sourceFileName(), "-O2", "-std=c++17", "-o", executableFileName());
            }

            @Override
            List<String> runCommand(JudgeProgram program) {
                return List.of(executableCommand());
            }
        },
        JAVA("Main.java") {
            @Override
            List<String> compileCommand(JudgeProgram program, Path workDir) {
                return List.of("javac", sourceFileName());
            }

            @Override
            List<String> runCommand(JudgeProgram program) {
                return List.of(
                        "java",
                        "-XX:+UseSerialGC",
                        "-XX:ActiveProcessorCount=1",
                        "-XX:MaxRAMPercentage=70.0",
                        "-cp",
                        ".",
                        "Main");
            }
        },
        PYTHON("main.py") {
            @Override
            List<String> compileCommand(JudgeProgram program, Path workDir) {
                return List.of();
            }

            @Override
            List<String> runCommand(JudgeProgram program) {
                return List.of(pythonCommand(), sourceFileName());
            }
        },
        GO("main.go") {
            @Override
            List<String> compileCommand(JudgeProgram program, Path workDir) {
                return List.of("go", "build", "-p=1", "-o", executableFileName(), sourceFileName());
            }

            @Override
            List<String> runCommand(JudgeProgram program) {
                return List.of(executableCommand());
            }
        };

        private final String sourceFileName;

        Language(String sourceFileName) {
            this.sourceFileName = sourceFileName;
        }

        String sourceFileName() {
            return sourceFileName;
        }

        abstract List<String> compileCommand(JudgeProgram program, Path workDir);

        abstract List<String> runCommand(JudgeProgram program);

        static Language from(String language) {
            String normalized = language == null ? "" : language.trim().toUpperCase(Locale.ROOT);
            return switch (normalized) {
                case "C++", "CPP", "CPLUSPLUS", "GNU C++" -> CPP;
                case "JAVA" -> JAVA;
                case "PYTHON", "PY", "PYTHON3" -> PYTHON;
                case "GO", "GOLANG" -> GO;
                default -> throw new IllegalArgumentException("Unsupported language: " + language);
            };
        }

        static Path executablePath(Path workDir) {
            return workDir.resolve(executableFileName()).normalize();
        }

        static String executableFileName() {
            return isWindows() ? "main.exe" : "main";
        }

        static String executableCommand() {
            return isWindows() ? ".\\main.exe" : "./main";
        }

        static boolean isWindows() {
            return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        }

        static String pythonCommand() {
            return isWindows() ? "python" : "python3";
        }
    }

    private record JudgeProgram(Language language, Path sourceFile, Path workDir) {
    }

    private record CompileResult(boolean success, String errorLog) {
    }

    private record RunResult(boolean timeout, int exitCode, byte[] stdout, String stderr, int timeUsedMs) {
    }

    private record JudgeRunSummary(
            String status,
            String message,
            String errorLog,
            int maxTimeMs,
            int maxMemoryKb,
            int score) {
    }
}
