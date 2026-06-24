package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.csuft.oj.dto.LoginRequest;
import com.csuft.oj.entity.RefreshToken;
import com.csuft.oj.entity.User;
import com.csuft.oj.mapper.RefreshTokenMapper;
import com.csuft.oj.mapper.UserMapper;
import com.csuft.oj.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    private AuthService service;

    @BeforeEach
    void setUp() {
        JwtUtils jwtUtils = new JwtUtils("test-jwt-secret-with-at-least-thirty-two-bytes", 900);
        service = new AuthService(userMapper, passwordEncoder, jwtUtils, refreshTokenMapper, 604800);
    }

    @Test
    void loginStoresOnlyHashedRefreshToken() {
        User user = activeUser();
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("password123", user.getPasswordHash())).thenReturn(true);

        AuthSession session = service.login(new LoginRequest("student", "password123"));

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenMapper).insert(tokenCaptor.capture());
        RefreshToken stored = tokenCaptor.getValue();
        assertNotNull(session.response().getToken());
        assertNotNull(session.refreshToken());
        assertNotEquals(session.refreshToken(), stored.getTokenHash());
        assertEquals(64, stored.getTokenHash().length());
        assertFalse(stored.getExpiresAt().isBefore(LocalDateTime.now().plusDays(6)));
    }

    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("student");
        user.setPasswordHash("encoded-password");
        user.setNickname("Student");
        user.setEmail("student@example.com");
        user.setRole("STUDENT");
        user.setStatus(1);
        return user;
    }
}
