package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.csuft.oj.dto.UserProfileUpdateRequest;
import com.csuft.oj.entity.User;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.mapper.SubmissionMapper;
import com.csuft.oj.mapper.UserMapper;
import com.csuft.oj.vo.SubmissionActivityVO;
import com.csuft.oj.vo.UserProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private SubmissionMapper submissionMapper;

    private UserProfileService service;

    @BeforeEach
    void setUp() {
        service = new UserProfileService(userMapper, submissionMapper);
    }

    @Test
    void profileContainsPersistedCountersAndActivity() {
        User user = activeUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(submissionMapper.selectDailyActivity(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(new SubmissionActivityVO(LocalDate.now(), 3)));

        UserProfileVO profile = service.getProfile(1L);

        assertEquals(7, profile.getGlobalAcCount());
        assertEquals(21, profile.getSubmitCount());
        assertEquals(3, profile.getSubmissionActivity().get(0).getCount());
    }

    @Test
    void updateRejectsDuplicateEmail() {
        when(userMapper.selectById(1L)).thenReturn(activeUser());
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setNickname("New name");
        request.setEmail("used@example.com");

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.updateProfile(1L, request));

        assertEquals(409, exception.getCode());
    }

    @Test
    void updatePersistsNormalizedProfile() {
        User user = activeUser();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(submissionMapper.selectDailyActivity(eq(1L), any(LocalDate.class))).thenReturn(List.of());
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setNickname("  New name  ");
        request.setEmail("New@Example.COM");

        UserProfileVO result = service.updateProfile(1L, request);

        verify(userMapper).updateById(user);
        assertEquals("New name", result.getNickname());
        assertEquals("new@example.com", result.getEmail());
    }

    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("student");
        user.setNickname("Student");
        user.setEmail("student@example.com");
        user.setRole("STUDENT");
        user.setGlobalAcCount(7);
        user.setSubmitCount(21);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now().minusYears(1));
        return user;
    }
}
