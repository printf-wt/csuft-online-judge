package com.csuft.oj.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = firstForwardedFor(request.getHeader("X-Forwarded-For"));
        if (hasText(forwardedFor)) {
            return forwardedFor;
        }
        String realIp = normalize(request.getHeader("X-Real-IP"));
        if (hasText(realIp)) {
            return realIp;
        }
        String remoteAddr = normalize(request.getRemoteAddr());
        return hasText(remoteAddr) ? remoteAddr : "unknown";
    }

    private static String firstForwardedFor(String value) {
        if (!hasText(value)) {
            return null;
        }
        for (String part : value.split(",")) {
            String candidate = normalize(part);
            if (hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return "unknown".equalsIgnoreCase(trimmed) ? null : trimmed;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
