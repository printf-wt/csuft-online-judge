package com.csuft.oj.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionSafetyValidatorTest {

    @Test
    void productionRejectsHostJudgeExecution() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                environment,
                "a-secure-random-jwt-secret-with-more-than-32-bytes",
                "a-secure-database-password",
                "HOST",
                "",
                true);

        assertThrows(IllegalStateException.class, validator::validate);
    }

    @Test
    void productionAcceptsConfiguredSandbox() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                environment,
                "a-secure-random-jwt-secret-with-more-than-32-bytes",
                "a-secure-database-password",
                "SANDBOX",
                "/usr/local/bin/csuft-oj-sandbox",
                true);

        assertDoesNotThrow(validator::validate);
    }

    @Test
    void productionRejectsInsecureRefreshCookie() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        ProductionSafetyValidator validator = new ProductionSafetyValidator(
                environment,
                "a-secure-random-jwt-secret-with-more-than-32-bytes",
                "a-secure-database-password",
                "SANDBOX",
                "/usr/local/bin/csuft-oj-sandbox",
                false);

        assertThrows(IllegalStateException.class, validator::validate);
    }
}
