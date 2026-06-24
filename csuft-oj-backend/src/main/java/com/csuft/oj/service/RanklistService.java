package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csuft.oj.entity.User;
import com.csuft.oj.mapper.UserMapper;
import com.csuft.oj.vo.PageResult;
import com.csuft.oj.vo.UserRankVO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global user ranklist query with a small local TTL cache.
 */
@Service
public class RanklistService {

    private static final Duration CACHE_TTL = Duration.ofSeconds(10);

    private final UserMapper userMapper;
    private final Map<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    public RanklistService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * Returns users ordered by solved count descending and submit count ascending.
     */
    public PageResult<UserRankVO> ranklist(long page, long size) {
        long safePage = Math.max(page, 1L);
        long safeSize = Math.min(Math.max(size, 1L), 100L);
        String cacheKey = safePage + ":" + safeSize;
        CacheEntry cached = localCache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.result();
        }

        Page<User> userPage = new Page<>(safePage, safeSize);
        Page<User> result = userMapper.selectPage(userPage, new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .orderByDesc(User::getGlobalAcCount)
                .orderByAsc(User::getSubmitCount)
                .orderByAsc(User::getId));

        long offset = (result.getCurrent() - 1) * result.getSize();
        List<UserRankVO> records = new ArrayList<>();
        for (int i = 0; i < result.getRecords().size(); i++) {
            User user = result.getRecords().get(i);
            records.add(new UserRankVO(
                        offset + i + 1,
                        user.getId(),
                        user.getUsername(),
                        user.getNickname(),
                        user.getGlobalAcCount(),
                        user.getSubmitCount()));
        }

        PageResult<UserRankVO> pageResult = new PageResult<>(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                records);
        localCache.put(cacheKey, new CacheEntry(pageResult, Instant.now().plus(CACHE_TTL)));
        return pageResult;
    }

    private record CacheEntry(PageResult<UserRankVO> result, Instant expiresAt) {
    }
}
