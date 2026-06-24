package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.csuft.oj.dto.ChangePasswordRequest;
import com.csuft.oj.entity.RefreshToken;
import com.csuft.oj.entity.User;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.mapper.RefreshTokenMapper;
import com.csuft.oj.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AccountSecurityService {

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;

    public AccountSecurityService(
            UserMapper userMapper,
            RefreshTokenMapper refreshTokenMapper,
            PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireActiveUser(userId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessException(400, "Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessException(400, "New password must be different from current password");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setTokenVersion(tokenVersion(user) + 1);
        user.setUpdatedAt(now);
        userMapper.updateById(user);
        revokeAllRefreshTokens(userId, now);
    }

    private User requireActiveUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "Authentication required");
        }
        User user = userMapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(401, "User account is unavailable");
        }
        return user;
    }

    private int tokenVersion(User user) {
        return user.getTokenVersion() == null ? 0 : user.getTokenVersion();
    }

    private void revokeAllRefreshTokens(Long userId, LocalDateTime now) {
        refreshTokenMapper.update(null, new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, userId)
                .isNull(RefreshToken::getRevokedAt)
                .set(RefreshToken::getRevokedAt, now));
    }
}
