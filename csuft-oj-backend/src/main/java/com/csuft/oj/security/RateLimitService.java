package com.csuft.oj.security;

import com.csuft.oj.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Single-instance fixed-window rate limiter. Replace with Redis for multiple API instances.
 */
@Service
public class RateLimitService {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();

    public void check(String key, int limit, Duration windowDuration) {
        if (limit <= 0) {
            return;
        }
        Instant now = Instant.now();
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || !current.expiresAt.isAfter(now)) {
                return new Window(1, now.plus(windowDuration));
            }
            return new Window(current.count + 1, current.expiresAt);
        });
        if (requestCount.incrementAndGet() % 1000 == 0) {
            windows.entrySet().removeIf(entry -> !entry.getValue().expiresAt.isAfter(now));
        }
        if (window.count > limit) {
            throw new BusinessException(429, "Too many requests. Please try again later");
        }
    }

    private record Window(int count, Instant expiresAt) {
    }
}
