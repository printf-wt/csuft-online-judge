package com.csuft.oj.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.csuft.oj.entity.AuditLog;
import com.csuft.oj.judge.JudgeDispatcher;
import com.csuft.oj.mapper.AuditLogMapper;
import com.csuft.oj.vo.AuditLogVO;
import com.csuft.oj.vo.PageResult;
import com.csuft.oj.vo.SystemMonitorVO;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.util.List;

/**
 * Admin-only system monitor and audit log queries.
 */
@Service
public class AdminSystemService {

    private final AuditLogMapper auditLogMapper;
    private final JudgeDispatcher judgeDispatcher;

    public AdminSystemService(AuditLogMapper auditLogMapper, JudgeDispatcher judgeDispatcher) {
        this.auditLogMapper = auditLogMapper;
        this.judgeDispatcher = judgeDispatcher;
    }

    public SystemMonitorVO monitor() {
        Runtime runtime = Runtime.getRuntime();
        double systemCpuLoad = -1D;
        double processCpuLoad = -1D;
        java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            systemCpuLoad = sunOsBean.getCpuLoad();
            processCpuLoad = sunOsBean.getProcessCpuLoad();
        }
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        int queueCapacity = judgeDispatcher.getQueueCapacity();
        int queueSize = judgeDispatcher.getQueueSize();
        return new SystemMonitorVO(
                runtime.maxMemory(),
                total,
                free,
                total - free,
                systemCpuLoad,
                processCpuLoad,
                queueSize,
                queueCapacity,
                queueCapacity == 0 ? 0D : (double) queueSize / queueCapacity,
                judgeDispatcher.getActiveWorkerCount(),
                judgeDispatcher.getWorkerCount(),
                judgeDispatcher.getScheduledTaskCount(),
                ManagementFactory.getRuntimeMXBean().getUptime(),
                runtime.availableProcessors(),
                System.currentTimeMillis());
    }

    public PageResult<AuditLogVO> auditLogs(long page, long size, String action) {
        Page<AuditLog> logPage = new Page<>(Math.max(page, 1L), Math.min(Math.max(size, 1L), 100L));
        LambdaQueryWrapper<AuditLog> wrapper = new LambdaQueryWrapper<>();
        if (action != null && !action.isBlank()) {
            wrapper.eq(AuditLog::getAction, action.trim());
        }
        wrapper.orderByDesc(AuditLog::getCreatedAt).orderByDesc(AuditLog::getId);
        Page<AuditLog> result = auditLogMapper.selectPage(logPage, wrapper);
        List<AuditLogVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), records);
    }

    private AuditLogVO toVO(AuditLog log) {
        return new AuditLogVO(
                log.getId(),
                log.getOperatorId(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getDetail(),
                log.getCreatedAt());
    }
}
