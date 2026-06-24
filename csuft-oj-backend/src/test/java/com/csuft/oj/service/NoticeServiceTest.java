package com.csuft.oj.service;

import com.csuft.oj.dto.NoticeCreateRequest;
import com.csuft.oj.entity.Notice;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.mapper.NoticeMapper;
import com.csuft.oj.vo.NoticeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeMapper noticeMapper;

    private NoticeService service;

    @BeforeEach
    void setUp() {
        service = new NoticeService(noticeMapper);
    }

    @Test
    void publicLookupHidesInvisibleNotice() {
        Notice notice = new Notice();
        notice.setId(1L);
        notice.setIsVisible(0);
        when(noticeMapper.selectById(1L)).thenReturn(notice);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.get(1L, false));

        assertEquals(404, exception.getCode());
    }

    @Test
    void createUsesSafeFlagDefaults() {
        NoticeCreateRequest request = new NoticeCreateRequest();
        request.setTitle("Maintenance");
        request.setContent("Tonight at 23:00");
        doAnswer(invocation -> {
            Notice notice = invocation.getArgument(0);
            notice.setId(10L);
            return 1;
        }).when(noticeMapper).insert(any(Notice.class));

        NoticeVO result = service.create(request, 2L);

        assertEquals(0, result.getIsPinned());
        assertEquals(1, result.getIsVisible());
        assertEquals(2L, result.getAuthorId());
    }
}
