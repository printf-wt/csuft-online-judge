package com.csuft.oj.observability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestTraceFilterTest {

    private final RequestTraceFilter filter = new RequestTraceFilter();

    @Test
    void keepsSafeCallerProvidedRequestId() {
        assertEquals("request-12345", filter.resolveRequestId("request-12345"));
    }

    @Test
    void replacesUnsafeRequestId() {
        String requestId = filter.resolveRequestId("bad id\nforged");

        assertNotEquals("bad id\nforged", requestId);
        assertTrue(requestId.length() >= 8);
    }
}
