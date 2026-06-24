package com.csuft.oj.observability;

import com.csuft.oj.judge.JudgeDispatcher;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class JudgeHealthIndicator implements HealthIndicator {

    private final JudgeDispatcher judgeDispatcher;

    public JudgeHealthIndicator(JudgeDispatcher judgeDispatcher) {
        this.judgeDispatcher = judgeDispatcher;
    }

    @Override
    public Health health() {
        Health.Builder builder = judgeDispatcher.isRunning() ? Health.up() : Health.down();
        return builder
                .withDetail("queueSize", judgeDispatcher.getQueueSize())
                .withDetail("queueCapacity", judgeDispatcher.getQueueCapacity())
                .withDetail("activeWorkers", judgeDispatcher.getActiveWorkerCount())
                .withDetail("workerCount", judgeDispatcher.getWorkerCount())
                .build();
    }
}
