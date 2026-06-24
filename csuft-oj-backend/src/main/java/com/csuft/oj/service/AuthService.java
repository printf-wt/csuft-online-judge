package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.csuft.oj.dto.LoginRequest;
import com.csuft.oj.dto.RegisterRequest;
import com.csuft.oj.entity.RefreshToken;
import com.csuft.oj.entity.User;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.mapper.RefreshTokenMapper;
import com.csuft.oj.mapper.UserMapper;
import com.csuft.oj.security.JwtUtils;
import com.csuft.oj.vo.AuthLoginVO;
import com.csuft.oj.vo.AuthUserVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Authentication use cases for registration and login.
 */
@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenMapper refreshTokenMapper;
    private final long refreshExpirationSeconds;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            RefreshTokenMapper refreshTokenMapper,
            @Value("${csuft-oj.security.jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.refreshTokenMapper = refreshTokenMapper;
        this.refreshExpirationSeconds = refreshExpirationSeconds;
    }

    /**
     * Registers a user after hashing the raw password with BCrypt.
     * Role is always forced to STUDENT for public registration.
     */
    public AuthUserVO register(RegisterRequest request) {
        String username = requireText(request.getUsername(), "Username cannot be empty");
        String password = requireText(request.getPassword(), "Password cannot be empty");
        String role = "STUDENT";

        if (existsByUsername(username)) {
            throw new BusinessException("Username already exists");
        }
        if (StringUtils.hasText(request.getEmail()) && existsByEmail(request.getEmail().trim())) {
            throw new BusinessException("Email already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname().trim() : username);
        user.setEmail(StringUtils.hasText(request.getEmail()) ? request.getEmail().trim() : null);
        user.setRole(role);
        user.setGlobalAcCount(0);
        user.setSubmitCount(0);
        user.setStatus(1);
        user.setTokenVersion(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userMapper.insert(user);
        return toAuthUserVO(user);
    }

    /**
     * Authenticates a user and returns a JWT token on success.
     */
    @Transactional(rollbackFor = Exception.class)
    public AuthSession login(LoginRequest request) {
        String username = requireText(request.getUsername(), "Username cannot be empty");
        String password = requireText(request.getPassword(), "Password cannot be empty");

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(401, "Invalid username or password");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "User account is disabled");
        }

        return createSession(user);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public AuthSession refresh(String rawRefreshToken) {
        String tokenHash = hashToken(requireText(rawRefreshToken, "Refresh token is required"));
        RefreshToken stored = refreshTokenMapper.selectOne(new LambdaQueryWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, tokenHash)
                .last("LIMIT 1"));
        LocalDateTime now = LocalDateTime.now();
        if (stored == null || !stored.getExpiresAt().isAfter(now)) {
            throw new BusinessException(401, "Refresh token is invalid or expired");
        }
        if (stored.getRevokedAt() != null) {
            revokeAllForUser(stored.getUserId(), now);
            throw new BusinessException(401, "Refresh token reuse detected");
        }

        User user = userMapper.selectById(stored.getUserId());
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            revokeAllForUser(stored.getUserId(), now);
            throw new BusinessException(401, "User account is unavailable");
        }

        String newRawToken = generateRefreshToken();
        String newTokenHash = hashToken(newRawToken);
        int updated = refreshTokenMapper.update(null, new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getId, stored.getId())
                .isNull(RefreshToken::getRevokedAt)
                .set(RefreshToken::getRevokedAt, now)
                .set(RefreshToken::getLastUsedAt, now)
                .set(RefreshToken::getReplacedByHash, newTokenHash));
        if (updated == 0) {
            revokeAllForUser(stored.getUserId(), now);
            throw new BusinessException(401, "Refresh token reuse detected");
        }
        insertRefreshToken(user.getId(), newTokenHash, now);
        return accessSession(user, newRawToken);
    }

    public void logout(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            return;
        }
        refreshTokenMapper.update(null, new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getTokenHash, hashToken(rawRefreshToken))
                .isNull(RefreshToken::getRevokedAt)
                .set(RefreshToken::getRevokedAt, LocalDateTime.now()));
    }

    private boolean existsByUsername(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        return count != null && count > 0;
    }

    private boolean existsByEmail(String email) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        return count != null && count > 0;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private AuthSession createSession(User user) {
        String refreshToken = generateRefreshToken();
        insertRefreshToken(user.getId(), hashToken(refreshToken), LocalDateTime.now());
        return accessSession(user, refreshToken);
    }

    private AuthSession accessSession(User user, String refreshToken) {
        String accessToken = jwtUtils.generateToken(
                user.getId(), user.getUsername(), user.getRole(), user.getTokenVersion());
        AuthLoginVO response = new AuthLoginVO(
                accessToken,
                "Bearer",
                jwtUtils.getExpirationSeconds(),
                toAuthUserVO(user));
        return new AuthSession(response, refreshToken, refreshExpirationSeconds);
    }

    private void insertRefreshToken(Long userId, String tokenHash, LocalDateTime now) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(now.plusSeconds(refreshExpirationSeconds));
        refreshToken.setCreatedAt(now);
        refreshTokenMapper.insert(refreshToken);
    }

    private void revokeAllForUser(Long userId, LocalDateTime now) {
        refreshTokenMapper.update(null, new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, userId)
                .isNull(RefreshToken::getRevokedAt)
                .set(RefreshToken::getRevokedAt, now));
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private AuthUserVO toAuthUserVO(User user) {
        return new AuthUserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getRole());
    }
}
