package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.csuft.oj.dto.UserProfileUpdateRequest;
import com.csuft.oj.entity.User;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.mapper.SubmissionMapper;
import com.csuft.oj.mapper.UserMapper;
import com.csuft.oj.vo.UserProfileVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class UserProfileService {

    private final UserMapper userMapper;
    private final SubmissionMapper submissionMapper;

    public UserProfileService(UserMapper userMapper, SubmissionMapper submissionMapper) {
        this.userMapper = userMapper;
        this.submissionMapper = submissionMapper;
    }

    public UserProfileVO getProfile(Long userId) {
        User user = requireActiveUser(userId);
        return toVO(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = requireActiveUser(userId);
        String email = request.getEmail().trim().toLowerCase();
        Long duplicateEmailCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)
                .ne(User::getId, userId));
        if (duplicateEmailCount != null && duplicateEmailCount > 0) {
            throw new BusinessException(409, "Email already exists");
        }

        user.setNickname(request.getNickname().trim());
        user.setEmail(email);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return toVO(user);
    }

    private User requireActiveUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(401, "Login required");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(403, "User account is disabled");
        }
        return user;
    }

    private UserProfileVO toVO(User user) {
        return new UserProfileVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getRole(),
                user.getGlobalAcCount(),
                user.getSubmitCount(),
                user.getCreatedAt(),
                submissionMapper.selectDailyActivity(user.getId(), LocalDate.now().minusDays(364)));
    }
}
