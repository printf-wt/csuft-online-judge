package com.csuft.oj.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * System monitor response data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemMonitorVO {

    private Long jvmMaxMemory;
    private Long jvmTotalMemory;
    private Long jvmFreeMemory;
    private Long jvmUsedMemory;
    private Double systemCpuLoad;
    private Double processCpuLoad;
    private Integer judgeQueueSize;
    private Integer judgeQueueCapacity;
    private Double judgeQueueUtilization;
    private Integer judgeActiveWorkers;
    private Integer judgeWorkerCount;
    private Integer judgeScheduledTasks;
    private Long uptimeMs;
    private Integer availableProcessors;
    private Long timestamp;
}
