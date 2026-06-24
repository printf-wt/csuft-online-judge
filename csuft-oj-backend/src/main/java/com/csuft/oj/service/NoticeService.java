package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csuft.oj.dto.NoticeCreateRequest;
import com.csuft.oj.dto.NoticeUpdateRequest;
import com.csuft.oj.entity.Notice;
import com.csuft.oj.exception.BusinessException;
import com.csuft.oj.mapper.NoticeMapper;
import com.csuft.oj.vo.NoticeVO;
import com.csuft.oj.vo.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;

    public NoticeService(NoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    public PageResult<NoticeVO> list(long page, long size, boolean includeHidden) {
        Page<Notice> request = new Page<>(Math.max(page, 1), Math.min(Math.max(size, 1), 100));
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        if (!includeHidden) {
            wrapper.eq(Notice::getIsVisible, 1);
        }
        wrapper.orderByDesc(Notice::getIsPinned)
                .orderByDesc(Notice::getCreatedAt)
                .orderByDesc(Notice::getId);
        Page<Notice> result = noticeMapper.selectPage(request, wrapper);
        List<NoticeVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), records);
    }

    public NoticeVO get(Long id, boolean includeHidden) {
        Notice notice = requireNotice(id);
        if (!includeHidden && !Integer.valueOf(1).equals(notice.getIsVisible())) {
            throw new BusinessException(404, "Notice not found");
        }
        return toVO(notice);
    }

    public NoticeVO create(NoticeCreateRequest request, Long authorId) {
        LocalDateTime now = LocalDateTime.now();
        Notice notice = new Notice();
        notice.setTitle(requireText(request.getTitle(), "Notice title cannot be empty"));
        notice.setContent(requireText(request.getContent(), "Notice content cannot be empty"));
        notice.setAuthorId(authorId);
        notice.setIsPinned(flag(request.getIsPinned(), 0));
        notice.setIsVisible(flag(request.getIsVisible(), 1));
        notice.setCreatedAt(now);
        notice.setUpdatedAt(now);
        noticeMapper.insert(notice);
        return toVO(notice);
    }

    public NoticeVO update(Long id, NoticeUpdateRequest request) {
        Notice notice = requireNotice(id);
        if (request.getTitle() != null) {
            notice.setTitle(requireText(request.getTitle(), "Notice title cannot be empty"));
        }
        if (request.getContent() != null) {
            notice.setContent(requireText(request.getContent(), "Notice content cannot be empty"));
        }
        if (request.getIsPinned() != null) {
            notice.setIsPinned(flag(request.getIsPinned(), 0));
        }
        if (request.getIsVisible() != null) {
            notice.setIsVisible(flag(request.getIsVisible(), 1));
        }
        notice.setUpdatedAt(LocalDateTime.now());
        noticeMapper.updateById(notice);
        return toVO(notice);
    }

    public void delete(Long id) {
        requireNotice(id);
        noticeMapper.deleteById(id);
    }

    private Notice requireNotice(Long id) {
        if (id == null) {
            throw new BusinessException("Notice ID cannot be empty");
        }
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(404, "Notice not found");
        }
        return notice;
    }

    private int flag(Integer value, int defaultValue) {
        int result = value == null ? defaultValue : value;
        if (result != 0 && result != 1) {
            throw new BusinessException("Flag must be 0 or 1");
        }
        return result;
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private NoticeVO toVO(Notice notice) {
        return new NoticeVO(
                notice.getId(), notice.getTitle(), notice.getContent(), notice.getAuthorId(),
                notice.getIsPinned(), notice.getIsVisible(), notice.getCreatedAt(), notice.getUpdatedAt());
    }
}
