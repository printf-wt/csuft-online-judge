package com.csuft.oj.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    @Test
    void usesFirstForwardedForAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.3");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.2");

        assertEquals("203.0.113.10", ClientIpResolver.resolve(request));
    }

    @Test
    void fallsBackToRealIpWhenForwardedForIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.3");
        request.addHeader("X-Real-IP", "198.51.100.7");

        assertEquals("198.51.100.7", ClientIpResolver.resolve(request));
    }

    @Test
    void ignoresUnknownForwardedValues() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.18.0.3");
        request.addHeader("X-Forwarded-For", "unknown, 198.51.100.8");

        assertEquals("198.51.100.8", ClientIpResolver.resolve(request));
    }

    @Test
    void fallsBackToRemoteAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertEquals("127.0.0.1", ClientIpResolver.resolve(request));
    }
}
