package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.csuft.oj.dto.ChangePasswordRequest;
import com.csuft.oj.entity.User;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.mapper.RefreshTokenMapper;
import com.csuft.oj.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSecurityServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RefreshTokenMapper refreshTokenMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountSecurityService service;

    @BeforeEach
    void setUp() {
        service = new AccountSecurityService(userMapper, refreshTokenMapper, passwordEncoder);
    }

    @Test
    void changePasswordInvalidatesAllExistingSessions() {
        User user = activeUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("old-password", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.changePassword(1L, request("old-password", "new-password"));

        assertEquals("new-hash", user.getPasswordHash());
        assertEquals(4, user.getTokenVersion());
        verify(userMapper).updateById(user);
        verify(refreshTokenMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        User user = activeUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.changePassword(1L, request("wrong-password", "new-password")));

        verify(userMapper, never()).updateById(any(User.class));
        verify(refreshTokenMapper, never()).update(any(), any(Wrapper.class));
    }

    private ChangePasswordRequest request(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("student");
        user.setPasswordHash("old-hash");
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setTokenVersion(3);
        return user;
    }
}
