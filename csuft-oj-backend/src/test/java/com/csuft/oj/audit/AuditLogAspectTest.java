package com.csuft.oj.audit;

import com.csuft.oj.dto.RegisterRequest;
import com.csuft.oj.mapper.AuditLogMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AuditLogAspectTest {

    private final AuditLogAspect aspect =
            new AuditLogAspect(mock(AuditLogMapper.class), new ObjectMapper());

    @Test
    void redactsSensitiveDtoFields() {
        RegisterRequest request = new RegisterRequest(
                "student",
                "password123",
                "Student",
                "student@example.com");

        JsonNode sanitized = aspect.sanitizeValue("request", request);

        assertEquals("[REDACTED]", sanitized.get("password").asText());
        assertEquals("student", sanitized.get("username").asText());
    }

    @Test
    void redactsSensitiveTopLevelArgument() {
        assertEquals("[REDACTED]", aspect.sanitizeValue("accessToken", "secret").asText());
    }
}
