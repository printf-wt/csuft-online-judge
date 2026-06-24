package com.csuft.oj.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Prevents production from starting with development credentials or host judge execution.
 */
@Component
public class ProductionSafetyValidator {

    private static final String DEVELOPMENT_JWT_SECRET =
            "local-development-jwt-secret-change-before-production";

    private final Environment environment;
    private final String jwtSecret;
    private final String databasePassword;
    private final String judgeExecutionMode;
    private final String sandboxCommand;
    private final boolean refreshCookieSecure;

    public ProductionSafetyValidator(
            Environment environment,
            @Value("${csuft-oj.security.jwt.secret}") String jwtSecret,
            @Value("${spring.datasource.password:}") String databasePassword,
            @Value("${csuft-oj.judge.execution-mode:HOST}") String judgeExecutionMode,
            @Value("${csuft-oj.judge.sandbox-command:}") String sandboxCommand,
            @Value("${csuft-oj.security.jwt.refresh-cookie-secure:false}") boolean refreshCookieSecure) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.databasePassword = databasePassword;
        this.judgeExecutionMode = judgeExecutionMode;
        this.sandboxCommand = sandboxCommand;
        this.refreshCookieSecure = refreshCookieSecure;
    }

    @PostConstruct
    public void validate() {
        boolean production = Arrays.stream(environment.getActiveProfiles())
                .anyMatch("prod"::equalsIgnoreCase);
        if (!production) {
            return;
        }
        if (databasePassword == null || databasePassword.isBlank()) {
            throw new IllegalStateException("DB_PASSWORD must be configured for production");
        }
        if (jwtSecret == null || jwtSecret.length() < 32 || DEVELOPMENT_JWT_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 random bytes for production");
        }
        if (!"SANDBOX".equalsIgnoreCase(judgeExecutionMode)) {
            throw new IllegalStateException(
                    "Production requires JUDGE_EXECUTION_MODE=SANDBOX; host execution is unsafe");
        }
        if (sandboxCommand == null || sandboxCommand.isBlank()) {
            throw new IllegalStateException("JUDGE_SANDBOX_COMMAND must be configured for production");
        }
        if (!refreshCookieSecure) {
            throw new IllegalStateException("JWT_REFRESH_COOKIE_SECURE must be true for production");
        }
    }
}
