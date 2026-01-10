package com.workflow.platform.model.dto;

import lombok.Data;

@Data
public class CoordinationStatsDTO {
    // 心跳统计
    private int totalClients;
    private int activeClients;
    private int healthyClients;
    private double heartbeatHealthRate;

    // 模式一致性统计
    private double modeConsistencyRate;
    private int inconsistencyCount;

    // 同步统计
    private int syncQueueSize;
    private int pendingSyncTasks;
    private int successSyncTasks;
    private int failedSyncTasks;

    // WebSocket统计
    private int webSocketConnections;

    // 时间戳
    private long lastUpdateTime;
}