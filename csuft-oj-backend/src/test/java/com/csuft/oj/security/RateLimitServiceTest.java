package com.csuft.oj.security;

import com.csuft.oj.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimitServiceTest {

    @Test
    void rejectsRequestsAfterConfiguredLimit() {
        RateLimitService service = new RateLimitService();

        assertDoesNotThrow(() -> service.check("login:user", 2, Duration.ofMinutes(1)));
        assertDoesNotThrow(() -> service.check("login:user", 2, Duration.ofMinutes(1)));
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.check("login:user", 2, Duration.ofMinutes(1)));

        assertEquals(429, exception.getCode());
    }
}
