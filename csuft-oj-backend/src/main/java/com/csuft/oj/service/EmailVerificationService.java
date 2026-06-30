package com.csuft.oj.service;

import com.csuft.oj.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-instance email code store for public registration.
 */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, VerificationCode> registerCodes = new ConcurrentHashMap<>();
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean mailEnabled;
    private final String mailFrom;
    private final Duration codeTtl;

    public EmailVerificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${csuft-oj.mail.enabled:false}") boolean mailEnabled,
            @Value("${csuft-oj.mail.from:}") String mailFrom,
            @Value("${csuft-oj.mail.register-code-ttl-seconds:600}") long codeTtlSeconds) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
        this.codeTtl = Duration.ofSeconds(codeTtlSeconds);
    }

    public void sendRegisterCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        cleanupExpiredCodes();
        String code = generateCode();
        Instant expiresAt = Instant.now().plus(codeTtl);
        registerCodes.put(normalizedEmail, new VerificationCode(code, expiresAt));

        if (!mailEnabled) {
            log.info("Registration verification code for {} is {}. It expires at {}.", normalizedEmail, code, expiresAt);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new BusinessException(500, "Email service is not configured");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(mailFrom)) {
            message.setFrom(mailFrom);
        }
        message.setTo(normalizedEmail);
        message.setSubject("CSUFT OJ registration verification code");
        message.setText("Your CSUFT OJ registration verification code is: " + code
                + "\n\nThis code expires in " + codeTtl.toMinutes() + " minutes.");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            registerCodes.remove(normalizedEmail);
            log.warn("Failed to send registration verification code to {}", normalizedEmail, ex);
            throw new BusinessException(502, "Email verification code could not be sent");
        }
    }

    public void verifyRegisterCode(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        VerificationCode stored = registerCodes.get(normalizedEmail);
        if (stored == null || stored.expiresAt().isBefore(Instant.now())) {
            registerCodes.remove(normalizedEmail);
            throw new BusinessException(400, "Email verification code is invalid or expired");
        }
        if (!stored.code().equals(code == null ? "" : code.trim())) {
            throw new BusinessException(400, "Email verification code is invalid or expired");
        }
        registerCodes.remove(normalizedEmail);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String generateCode() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private void cleanupExpiredCodes() {
        Instant now = Instant.now();
        registerCodes.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record VerificationCode(String code, Instant expiresAt) {
    }
}
