package com.csuft.oj.controller;

import com.csuft.oj.dto.LoginRequest;
import com.csuft.oj.dto.RegisterEmailCodeRequest;
import com.csuft.oj.dto.RegisterRequest;
import com.csuft.oj.security.ClientIpResolver;
import com.csuft.oj.security.RateLimitService;
import com.csuft.oj.service.AuthSession;
import com.csuft.oj.service.AuthService;
import com.csuft.oj.vo.ApiResponse;
import com.csuft.oj.vo.AuthLoginVO;
import com.csuft.oj.vo.AuthUserVO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication API for registration and login.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "csuft_oj_refresh";

    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final boolean refreshCookieSecure;
    private final int loginAttempts;
    private final long loginWindowSeconds;
    private final int registerAttempts;
    private final long registerWindowSeconds;
    private final int registerCodeAttempts;
    private final long registerCodeWindowSeconds;

    public AuthController(
            AuthService authService,
            RateLimitService rateLimitService,
            @Value("${csuft-oj.security.jwt.refresh-cookie-secure:false}") boolean refreshCookieSecure,
            @Value("${csuft-oj.rate-limit.login-attempts:10}") int loginAttempts,
            @Value("${csuft-oj.rate-limit.login-window-seconds:300}") long loginWindowSeconds,
            @Value("${csuft-oj.rate-limit.register-attempts:5}") int registerAttempts,
            @Value("${csuft-oj.rate-limit.register-window-seconds:3600}") long registerWindowSeconds,
            @Value("${csuft-oj.rate-limit.register-code-attempts:5}") int registerCodeAttempts,
            @Value("${csuft-oj.rate-limit.register-code-window-seconds:300}") long registerCodeWindowSeconds) {
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.refreshCookieSecure = refreshCookieSecure;
        this.loginAttempts = loginAttempts;
        this.loginWindowSeconds = loginWindowSeconds;
        this.registerAttempts = registerAttempts;
        this.registerWindowSeconds = registerWindowSeconds;
        this.registerCodeAttempts = registerCodeAttempts;
        this.registerCodeWindowSeconds = registerCodeWindowSeconds;
    }

    /**
     * Sends the email verification code used by registration.
     */
    @PostMapping("/register-code")
    public ApiResponse<Void> sendRegisterCode(
            @Valid @RequestBody RegisterEmailCodeRequest request,
            HttpServletRequest servletRequest) {
        rateLimitService.check(
                "register-code:" + ClientIpResolver.resolve(servletRequest),
                registerCodeAttempts,
                java.time.Duration.ofSeconds(registerCodeWindowSeconds));
        authService.sendRegisterCode(request);
        return ApiResponse.success(null);
    }

    /**
     * Registers a new user after email code verification and stores the BCrypt password hash in tb_user.
     */
    @PostMapping("/register")
    public ApiResponse<AuthUserVO> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        rateLimitService.check(
                "register:" + ClientIpResolver.resolve(servletRequest),
                registerAttempts,
                java.time.Duration.ofSeconds(registerWindowSeconds));
        return ApiResponse.success(authService.register(request));
    }

    /**
     * Logs in and returns a JWT token with user profile data.
     */
    @PostMapping("/login")
    public ApiResponse<AuthLoginVO> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim().toLowerCase();
        rateLimitService.check(
                "login:" + ClientIpResolver.resolve(servletRequest) + ":" + username,
                loginAttempts,
                java.time.Duration.ofSeconds(loginWindowSeconds));
        AuthSession session = authService.login(request);
        setRefreshCookie(servletResponse, session);
        return ApiResponse.success(session.response());
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthLoginVO> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthSession session = authService.refresh(readRefreshCookie(request));
        setRefreshCookie(response, session);
        return ApiResponse.success(session.response());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(readRefreshCookie(request));
        clearRefreshCookie(response);
        return ApiResponse.success(null);
    }

    private String readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void setRefreshCookie(HttpServletResponse response, AuthSession session) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, session.refreshToken())
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(session.refreshExpiresInSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
