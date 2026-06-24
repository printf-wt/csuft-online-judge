package com.csuft.oj.judge;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csuft.oj.entity.Submission;
import com.csuft.oj.mapper.SubmissionMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bounded asynchronous judge dispatcher backed by recoverable database states.
 */
@Slf4j
@Component
public class JudgeDispatcher implements JudgeTaskPublisher {

    private static final List<String> RECOVERABLE_STATUSES =
            List.of("PENDING", "JUDGING", "COMPILING", "RUNNING");

    private final JudgeService judgeService;
    private final SubmissionMapper submissionMapper;
    private final LinkedBlockingQueue<Long> submissionQueue;
    private final Set<Long> scheduledSubmissionIds = ConcurrentHashMap.newKeySet();
    private final List<Thread> workerThreads = new ArrayList<>();
    private final AtomicInteger activeWorkerCount = new AtomicInteger();
    private final Counter rejectedTasks;
    private final Counter completedTasks;
    private final Counter failedTasks;
    private final int queueCapacity;
    private final int workerCount;
    private final boolean recoveryEnabled;

    private volatile boolean running;

    public JudgeDispatcher(
            JudgeService judgeService,
            SubmissionMapper submissionMapper,
            MeterRegistry meterRegistry,
            @Value("${csuft-oj.judge.queue-capacity:1000}") int queueCapacity,
            @Value("${csuft-oj.judge.worker-count:2}") int workerCount,
            @Value("${csuft-oj.judge.recovery-enabled:true}") boolean recoveryEnabled) {
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("Judge queue capacity must be positive");
        }
        if (workerCount <= 0) {
            throw new IllegalArgumentException("Judge worker count must be positive");
        }
        this.judgeService = judgeService;
        this.submissionMapper = submissionMapper;
        this.submissionQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.queueCapacity = queueCapacity;
        this.workerCount = workerCount;
        this.recoveryEnabled = recoveryEnabled;
        this.rejectedTasks = meterRegistry.counter("csuft_oj_judge_tasks_rejected_total");
        this.completedTasks = meterRegistry.counter("csuft_oj_judge_tasks_completed_total");
        this.failedTasks = meterRegistry.counter("csuft_oj_judge_tasks_failed_total");
        Gauge.builder("csuft_oj_judge_queue_size", submissionQueue, queue -> queue.size())
                .description("Current number of submissions waiting for judging")
                .register(meterRegistry);
        Gauge.builder("csuft_oj_judge_workers_active", activeWorkerCount, AtomicInteger::get)
                .description("Current number of active judge workers")
                .register(meterRegistry);
        Gauge.builder("csuft_oj_judge_tasks_scheduled", scheduledSubmissionIds, Set::size)
                .description("Current number of queued or running submission IDs")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        running = true;
        recoverPendingTasks();
        for (int i = 1; i <= workerCount; i++) {
            Thread worker = new Thread(this::workerLoop, "judge-worker-" + i);
            worker.setDaemon(true);
            worker.start();
            workerThreads.add(worker);
        }
        log.info("Judge dispatcher started with {} workers and queue capacity {}",
                workerCount, submissionQueue.remainingCapacity() + submissionQueue.size());
    }

    /**
     * Enqueues a task when capacity is available. A full queue is recovered from the database later.
     */
    @Override
    public boolean addSubmissionTask(Long submissionId) {
        if (submissionId == null) {
            throw new IllegalArgumentException("submissionId cannot be null");
        }
        if (!scheduledSubmissionIds.add(submissionId)) {
            return true;
        }
        if (submissionQueue.offer(submissionId)) {
            return true;
        }
        scheduledSubmissionIds.remove(submissionId);
        rejectedTasks.increment();
        log.warn("Judge queue is full; submission {} remains recoverable in the database", submissionId);
        return false;
    }

    public int getQueueSize() {
        return submissionQueue.size();
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public int getActiveWorkerCount() {
        return activeWorkerCount.get();
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public int getScheduledTaskCount() {
        return scheduledSubmissionIds.size();
    }

    public boolean isRunning() {
        return running;
    }

    private void workerLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            Long submissionId = null;
            try {
                submissionId = submissionQueue.take();
                activeWorkerCount.incrementAndGet();
                judgeService.judge(submissionId);
                completedTasks.increment();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                failedTasks.increment();
                log.error("Judge worker failed", ex);
            } finally {
                if (submissionId != null) {
                    activeWorkerCount.decrementAndGet();
                }
                if (submissionId != null) {
                    scheduledSubmissionIds.remove(submissionId);
                }
                if (running) {
                    try {
                        recoverPendingTasks();
                    } catch (Exception ex) {
                        log.error("Failed to recover pending judge tasks", ex);
                    }
                }
            }
        }
    }

    private synchronized void recoverPendingTasks() {
        int available = submissionQueue.remainingCapacity();
        if (!running || available <= 0) {
            return;
        }
        if (!recoveryEnabled) {
            return;
        }
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .in(Submission::getStatus, RECOVERABLE_STATUSES);
        if (!scheduledSubmissionIds.isEmpty()) {
            wrapper.notIn(Submission::getId, List.copyOf(scheduledSubmissionIds));
        }
        wrapper.orderByAsc(Submission::getId).last("LIMIT " + available);
        List<Submission> submissions = submissionMapper.selectList(wrapper);
        for (Submission submission : submissions) {
            addSubmissionTask(submission.getId());
        }
    }

    @PreDestroy
    public void destroy() {
        running = false;
        workerThreads.forEach(Thread::interrupt);
        workerThreads.clear();
        log.info("Judge dispatcher stopped; unfinished tasks remain recoverable in the database");
    }
}
