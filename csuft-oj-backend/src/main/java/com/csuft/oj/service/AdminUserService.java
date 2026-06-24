package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csuft.oj.dto.AdminUserUpdateRequest;
import com.csuft.oj.entity.RefreshToken;
import com.csuft.oj.entity.User;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.mapper.RefreshTokenMapper;
import com.csuft.oj.mapper.UserMapper;
import com.csuft.oj.vo.AdminUserVO;
import com.csuft.oj.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class AdminUserService {

    private static final Set<String> ROLES = Set.of("STUDENT", "TEACHER", "ADMIN");

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;

    public AdminUserService(UserMapper userMapper, RefreshTokenMapper refreshTokenMapper) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
    }

    public PageResult<AdminUserVO> list(long page, long size, String keyword, String role, Integer status) {
        Page<User> queryPage = new Page<>(Math.max(page, 1L), Math.min(Math.max(size, 1L), 100L));
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String value = keyword.trim();
            wrapper.and(query -> query.like(User::getUsername, value)
                    .or().like(User::getNickname, value)
                    .or().like(User::getEmail, value));
        }
        if (StringUtils.hasText(role)) {
            String normalizedRole = role.trim().toUpperCase();
            if (!ROLES.contains(normalizedRole)) {
                throw new BusinessException("Role is invalid");
            }
            wrapper.eq(User::getRole, normalizedRole);
        }
        if (status != null) {
            if (status != 0 && status != 1) {
                throw new BusinessException("Status is invalid");
            }
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreatedAt).orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(queryPage, wrapper);
        List<AdminUserVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), records);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminUserVO update(Long operatorId, Long userId, AdminUserUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        String role = request.getRole().trim().toUpperCase();
        if (operatorId != null && operatorId.equals(userId)
                && (!"ADMIN".equals(role) || !Integer.valueOf(1).equals(request.getStatus()))) {
            throw new BusinessException("You cannot disable or demote your own administrator account");
        }

        boolean securityChanged = !role.equals(user.getRole())
                || !request.getStatus().equals(user.getStatus());
        user.setRole(role);
        user.setStatus(request.getStatus());
        user.setUpdatedAt(LocalDateTime.now());
        if (securityChanged) {
            user.setTokenVersion((user.getTokenVersion() == null ? 0 : user.getTokenVersion()) + 1);
        }
        userMapper.updateById(user);
        if (securityChanged) {
            revokeAllRefreshTokens(userId, user.getUpdatedAt());
        }
        return toVO(user);
    }

    private void revokeAllRefreshTokens(Long userId, LocalDateTime now) {
        refreshTokenMapper.update(null, new LambdaUpdateWrapper<RefreshToken>()
                .eq(RefreshToken::getUserId, userId)
                .isNull(RefreshToken::getRevokedAt)
                .set(RefreshToken::getRevokedAt, now));
    }

    private AdminUserVO toVO(User user) {
        return new AdminUserVO(
                user.getId(), user.getUsername(), user.getNickname(), user.getEmail(), user.getRole(),
                user.getStatus(), user.getGlobalAcCount(), user.getSubmitCount(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
