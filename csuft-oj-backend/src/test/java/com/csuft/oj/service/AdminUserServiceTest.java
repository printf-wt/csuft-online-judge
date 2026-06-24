package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.csuft.oj.dto.AdminUserUpdateRequest;
import com.csuft.oj.entity.User;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.mapper.RefreshTokenMapper;
import com.csuft.oj.mapper.UserMapper;
import com.csuft.oj.vo.AdminUserVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    private AdminUserService service;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userMapper, refreshTokenMapper);
    }

    @Test
    void accessChangeInvalidatesTargetSessions() {
        User user = user(2L, "STUDENT", 1);
        when(userMapper.selectById(2L)).thenReturn(user);

        AdminUserVO result = service.update(1L, 2L, request("TEACHER", 1));

        assertEquals("TEACHER", result.getRole());
        assertEquals(1, user.getTokenVersion());
        verify(userMapper).updateById(user);
        verify(refreshTokenMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void administratorCannotDisableOwnAccount() {
        User user = user(1L, "ADMIN", 1);
        when(userMapper.selectById(1L)).thenReturn(user);

        assertThrows(BusinessException.class, () -> service.update(1L, 1L, request("ADMIN", 0)));

        verify(userMapper, never()).updateById(any(User.class));
        verify(refreshTokenMapper, never()).update(any(), any(Wrapper.class));
    }

    private AdminUserUpdateRequest request(String role, int status) {
        AdminUserUpdateRequest request = new AdminUserUpdateRequest();
        request.setRole(role);
        request.setStatus(status);
        return request;
    }

    private User user(Long id, String role, int status) {
        User user = new User();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setRole(role);
        user.setStatus(status);
        user.setTokenVersion(0);
        return user;
    }
}
