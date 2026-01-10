package com.workflow.platform.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.platform.enums.ModeType;
import com.workflow.platform.model.dto.HeartbeatDTO;
import com.workflow.platform.model.dto.SystemStatusDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 心跳管理器 - 管理客户端心跳和状态同步
 */
@Slf4j
@Component
public class HeartbeatManager {

    @Autowired
    private ModeManager modeManager;

    @Autowired
    private ModeConsistencyChecker modeConsistencyChecker;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${workflow.platform.heartbeat.interval:30000}")
    private long heartbeatInterval;

    @Value("${workflow.platform.heartbeat.timeout:90000}")
    private long heartbeatTimeout;

    @Value("${workflow.platform.heartbeat.max-missed:3}")
    private int maxMissedHeartbeats;

    @Value("${workflow.platform.heartbeat.enable-broadcast:true}")
    private boolean enableBroadcast;

    // 客户端心跳记录
    private final ConcurrentHashMap<String, HeartbeatRecord> heartbeatRecords =
            new ConcurrentHashMap<>();

    // 调度器
    private ScheduledExecutorService heartbeatScheduler;
    private ScheduledExecutorService cleanupScheduler;

    // 统计
    private final AtomicLong totalHeartbeats = new AtomicLong(0);
    private final AtomicLong missedHeartbeats = new AtomicLong(0);

    @PostConstruct
    public void init() {
        log.info("初始化心跳管理器");

        // 创建调度器
        heartbeatScheduler = Executors.newScheduledThreadPool(2);
        cleanupScheduler = Executors.newScheduledThreadPool(1);

        // 启动心跳检查任务
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                checkHeartbeats();
            } catch (Exception e) {
                log.error("心跳检查异常: {}", e.getMessage(), e);
            }
        }, 10, heartbeatInterval, TimeUnit.MILLISECONDS);

        // 启动清理任务
        cleanupScheduler.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredRecords();
            } catch (Exception e) {
                log.error("清理心跳记录异常: {}", e.getMessage(), e);
            }
        }, 60, 60, TimeUnit.SECONDS);

        log.info("心跳管理器初始化完成，检查间隔: {}ms，超时时间: {}ms",
                heartbeatInterval, heartbeatTimeout);
    }

    @PreDestroy
    public void shutdown() {
        log.info("关闭心跳管理器");

        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdown();
            try {
                if (!heartbeatScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    heartbeatScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (cleanupScheduler != null) {
            cleanupScheduler.shutdownNow();
        }

        log.info("心跳管理器已关闭");
    }

    /**
     * 处理心跳
     */
    public HeartbeatResponse processHeartbeat(HeartbeatDTO heartbeat) {
        String clientId = heartbeat.getClientId();
        long currentTime = System.currentTimeMillis();

        // 获取或创建心跳记录
        HeartbeatRecord record = heartbeatRecords.computeIfAbsent(clientId,
                k -> new HeartbeatRecord(clientId));

        // 更新记录
        record.setLastHeartbeatTime(currentTime);
        record.setClientMode(heartbeat.getMode());
        record.setClientVersion(heartbeat.getClientVersion());
        record.setClientPlatform(heartbeat.getPlatform());
        record.setSessionId(heartbeat.getSessionId());
        record.setMissedCount(0);
        record.setActive(true);

        // 更新统计
        totalHeartbeats.incrementAndGet();

        // 注册客户端模式
        modeConsistencyChecker.registerClientMode(
                clientId, heartbeat.getMode(), heartbeat.getSessionId());

        // 准备响应
        HeartbeatResponse response = new HeartbeatResponse();
        response.setTimestamp(currentTime);
        response.setServerTime(currentTime);
        response.setServerMode(modeManager.getCurrentMode());
        response.setNextHeartbeatInterval(heartbeatInterval);

        // 检查是否需要状态同步
        if (needsStateSync(record, heartbeat)) {
            response.setNeedsSync(true);
            response.setSyncData(prepareSyncData(clientId));
            log.debug("客户端 {} 需要状态同步", clientId);
        }

        // 检查模式一致性
        boolean consistent = modeConsistencyChecker.checkConsistency(heartbeat.getMode());
        response.setModeConsistent(consistent);

        if (!consistent) {
            response.setSuggestedMode(modeManager.getCurrentMode());
            log.warn("客户端 {} 模式不一致: 客户端={}, 服务器={}",
                    clientId, heartbeat.getMode(), modeManager.getCurrentMode());
        }

        // 广播系统状态（如果需要）
        if (enableBroadcast && record.isNeedsBroadcast()) {
            broadcastSystemStatus();
            record.setNeedsBroadcast(false);
        }

        log.debug("处理心跳: {}，最后心跳时间: {}", clientId, currentTime);

        return response;
    }

    /**
     * 获取客户端心跳状态
     */
    public HeartbeatStatus getClientStatus(String clientId) {
        HeartbeatRecord record = heartbeatRecords.get(clientId);
        if (record == null) {
            return HeartbeatStatus.NOT_FOUND;
        }

        long currentTime = System.currentTimeMillis();
        long lastHeartbeatTime = record.getLastHeartbeatTime();

        if (!record.isActive()) {
            return HeartbeatStatus.INACTIVE;
        }

        if (currentTime - lastHeartbeatTime > heartbeatTimeout) {
            return HeartbeatStatus.TIMEOUT;
        }

        if (record.getMissedCount() > 0) {
            return HeartbeatStatus.MISSING;
        }

        return HeartbeatStatus.HEALTHY;
    }

    /**
     * 获取所有客户端状态
     */
    public java.util.Map<String, HeartbeatStatus> getAllClientStatus() {
        java.util.Map<String, HeartbeatStatus> statusMap = new java.util.HashMap<>();

        for (String clientId : heartbeatRecords.keySet()) {
            statusMap.put(clientId, getClientStatus(clientId));
        }

        return statusMap;
    }

    /**
     * 获取活跃客户端列表
     */
    public java.util.List<String> getActiveClients() {
        java.util.List<String> activeClients = new java.util.ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (java.util.Map.Entry<String, HeartbeatRecord> entry : heartbeatRecords.entrySet()) {
            HeartbeatRecord record = entry.getValue();
            if (record.isActive() &&
                    currentTime - record.getLastHeartbeatTime() <= heartbeatTimeout) {
                activeClients.add(entry.getKey());
            }
        }

        return activeClients;
    }

    /**
     * 强制客户端离线
     */
    public boolean forceClientOffline(String clientId) {
        HeartbeatRecord record = heartbeatRecords.get(clientId);
        if (record == null) {
            return false;
        }

        record.setActive(false);
        record.setForcedOffline(true);
        record.setForcedOfflineTime(System.currentTimeMillis());

        log.info("强制客户端离线: {}", clientId);
        return true;
    }

    /**
     * 获取心跳统计
     */
    public HeartbeatStats getStats() {
        HeartbeatStats stats = new HeartbeatStats();

        stats.setTotalClients(heartbeatRecords.size());
        stats.setActiveClients(getActiveClients().size());
        stats.setTotalHeartbeats(totalHeartbeats.get());
        stats.setMissedHeartbeats(missedHeartbeats.get());
        stats.setLastUpdateTime(System.currentTimeMillis());

        // 计算健康度
        int healthyClients = 0;
        long currentTime = System.currentTimeMillis();

        for (HeartbeatRecord record : heartbeatRecords.values()) {
            if (record.isActive() &&
                    currentTime - record.getLastHeartbeatTime() <= heartbeatInterval * 2) {
                healthyClients++;
            }
        }

        stats.setHealthyClients(healthyClients);
        stats.setHealthRate(heartbeatRecords.size() > 0 ?
                (double) healthyClients / heartbeatRecords.size() : 1.0);

        return stats;
    }

    /**
     * 发送系统状态广播
     */
    public void broadcastSystemStatus() {
        try {
            SystemStatusDTO status = prepareSystemStatus();

            // 在实际应用中，这里应该通过WebSocket或消息队列广播
            // 这里简化为日志记录
            log.info("广播系统状态: {}", status);

            // 标记所有客户端需要接收广播
            for (HeartbeatRecord record : heartbeatRecords.values()) {
                record.setNeedsBroadcast(true);
            }

        } catch (Exception e) {
            log.error("广播系统状态失败: {}", e.getMessage(), e);
        }
    }

    // ========== 私有方法 ==========

    private void checkHeartbeats() {
        long currentTime = System.currentTimeMillis();
        int missedCount = 0;

        for (HeartbeatRecord record : heartbeatRecords.values()) {
            if (!record.isActive()) {
                continue;
            }

            long lastHeartbeat = record.getLastHeartbeatTime();
            long timeSinceLastHeartbeat = currentTime - lastHeartbeat;

            if (timeSinceLastHeartbeat > heartbeatInterval) {
                record.incrementMissedCount();
                missedCount++;

                log.debug("客户端 {} 错过心跳，已错过 {} 次",
                        record.getClientId(), record.getMissedCount());

                // 检查是否超过最大错过次数
                if (record.getMissedCount() >= maxMissedHeartbeats) {
                    handleClientTimeout(record);
                }
            } else {
                // 重置错过计数
                if (record.getMissedCount() > 0) {
                    record.setMissedCount(0);
                }
            }
        }

        if (missedCount > 0) {
            missedHeartbeats.addAndGet(missedCount);
            log.warn("心跳检查: {} 个客户端错过心跳", missedCount);
        }
    }

    private void handleClientTimeout(HeartbeatRecord record) {
        log.warn("客户端心跳超时: {}，最后心跳时间: {}",
                record.getClientId(), record.getLastHeartbeatTime());

        record.setActive(false);
        record.setTimeoutTime(System.currentTimeMillis());

        // 触发超时事件
        triggerClientTimeoutEvent(record);

        // 发送通知
        sendTimeoutNotification(record);
    }

    private void cleanupExpiredRecords() {
        long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24小时
        int removed = 0;

        for (String clientId : heartbeatRecords.keySet()) {
            HeartbeatRecord record = heartbeatRecords.get(clientId);

            // 移除非活跃且超过24小时的记录
            if (!record.isActive() && record.getLastHeartbeatTime() < cutoffTime) {
                heartbeatRecords.remove(clientId);
                removed++;
                log.debug("清理过期心跳记录: {}", clientId);
            }
        }

        if (removed > 0) {
            log.info("清理心跳记录完成，移除 {} 条", removed);
        }
    }

    private boolean needsStateSync(HeartbeatRecord record, HeartbeatDTO heartbeat) {
        // 检查是否需要状态同步的条件：
        // 1. 客户端首次连接
        // 2. 模式发生变化
        // 3. 超过同步时间间隔
        // 4. 服务器状态有重要更新

        long currentTime = System.currentTimeMillis();

        // 首次连接
        if (record.getFirstHeartbeatTime() == 0) {
            record.setFirstHeartbeatTime(currentTime);
            return true;
        }

        // 模式变化
        if (record.getClientMode() != null &&
                record.getClientMode() != heartbeat.getMode()) {
            return true;
        }

        // 同步时间间隔（默认5分钟）
        long syncInterval = 5 * 60 * 1000;
        if (currentTime - record.getLastSyncTime() > syncInterval) {
            return true;
        }

        // 服务器有重要更新
        if (record.isServerUpdated()) {
            return true;
        }

        return false;
    }

    private SystemStatusDTO prepareSyncData(String clientId) {
        SystemStatusDTO status = prepareSystemStatus();

        // 添加客户端特定的信息
        status.setClientId(clientId);
        status.setSyncTime(System.currentTimeMillis());

        return status;
    }

    private SystemStatusDTO prepareSystemStatus() {
        SystemStatusDTO status = new SystemStatusDTO();

        status.setServerTime(System.currentTimeMillis());
        status.setServerMode(modeManager.getCurrentMode());
        status.setServerVersion("1.0.0");
        status.setSystemStatus("healthy");

        // 获取系统统计
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("activeClients", getActiveClients().size());
        stats.put("totalHeartbeats", totalHeartbeats.get());
        stats.put("uptime", getSystemUptime());

        status.setStatistics(stats);

        return status;
    }

    private long getSystemUptime() {
        // 在实际应用中，这里应该返回系统运行时间
        // 这里简化为固定值
        return 24 * 60 * 60 * 1000L; // 24小时
    }

    private void triggerClientTimeoutEvent(HeartbeatRecord record) {
        // 触发客户端超时事件
        log.warn("客户端超时事件: {}", record.getClientId());
    }

    private void sendTimeoutNotification(HeartbeatRecord record) {
        // 发送超时通知
        log.info("发送客户端超时通知: {}", record.getClientId());
    }

    // ========== 内部类 ==========

    /**
     * 心跳记录
     */
    public static class HeartbeatRecord {
        private final String clientId;
        private long firstHeartbeatTime;
        private long lastHeartbeatTime;
        private ModeType clientMode;
        private String clientVersion;
        private String clientPlatform;
        private String sessionId;
        private int missedCount;
        private boolean active;
        private boolean forcedOffline;
        private long forcedOfflineTime;
        private long timeoutTime;
        private long lastSyncTime;
        private boolean needsBroadcast;
        private boolean serverUpdated;

        public HeartbeatRecord(String clientId) {
            this.clientId = clientId;
            this.firstHeartbeatTime = 0;
            this.lastHeartbeatTime = 0;
            this.missedCount = 0;
            this.active = true;
            this.forcedOffline = false;
            this.needsBroadcast = true;
            this.serverUpdated = false;
        }

        // Getters and Setters
        public String getClientId() { return clientId; }

        public long getFirstHeartbeatTime() { return firstHeartbeatTime; }
        public void setFirstHeartbeatTime(long firstHeartbeatTime) {
            this.firstHeartbeatTime = firstHeartbeatTime;
        }

        public long getLastHeartbeatTime() { return lastHeartbeatTime; }
        public void setLastHeartbeatTime(long lastHeartbeatTime) {
            this.lastHeartbeatTime = lastHeartbeatTime;
        }

        public ModeType getClientMode() { return clientMode; }
        public void setClientMode(ModeType clientMode) { this.clientMode = clientMode; }

        public String getClientVersion() { return clientVersion; }
        public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }

        public String getClientPlatform() { return clientPlatform; }
        public void setClientPlatform(String clientPlatform) { this.clientPlatform = clientPlatform; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public int getMissedCount() { return missedCount; }
        public void setMissedCount(int missedCount) { this.missedCount = missedCount; }
        public void incrementMissedCount() { this.missedCount++; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        public boolean isForcedOffline() { return forcedOffline; }
        public void setForcedOffline(boolean forcedOffline) { this.forcedOffline = forcedOffline; }

        public long getForcedOfflineTime() { return forcedOfflineTime; }
        public void setForcedOfflineTime(long forcedOfflineTime) {
            this.forcedOfflineTime = forcedOfflineTime;
        }

        public long getTimeoutTime() { return timeoutTime; }
        public void setTimeoutTime(long timeoutTime) { this.timeoutTime = timeoutTime; }

        public long getLastSyncTime() { return lastSyncTime; }
        public void setLastSyncTime(long lastSyncTime) { this.lastSyncTime = lastSyncTime; }

        public boolean isNeedsBroadcast() { return needsBroadcast; }
        public void setNeedsBroadcast(boolean needsBroadcast) { this.needsBroadcast = needsBroadcast; }

        public boolean isServerUpdated() { return serverUpdated; }
        public void setServerUpdated(boolean serverUpdated) { this.serverUpdated = serverUpdated; }
    }

    /**
     * 心跳响应
     */
    public static class HeartbeatResponse {
        private long timestamp;
        private long serverTime;
        private ModeType serverMode;
        private long nextHeartbeatInterval;
        private boolean needsSync;
        private SystemStatusDTO syncData;
        private boolean modeConsistent;
        private ModeType suggestedMode;

        // Getters and Setters
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public long getServerTime() { return serverTime; }
        public void setServerTime(long serverTime) { this.serverTime = serverTime; }

        public ModeType getServerMode() { return serverMode; }
        public void setServerMode(ModeType serverMode) { this.serverMode = serverMode; }

        public long getNextHeartbeatInterval() { return nextHeartbeatInterval; }
        public void setNextHeartbeatInterval(long nextHeartbeatInterval) {
            this.nextHeartbeatInterval = nextHeartbeatInterval;
        }

        public boolean isNeedsSync() { return needsSync; }
        public void setNeedsSync(boolean needsSync) { this.needsSync = needsSync; }

        public SystemStatusDTO getSyncData() { return syncData; }
        public void setSyncData(SystemStatusDTO syncData) { this.syncData = syncData; }

        public boolean isModeConsistent() { return modeConsistent; }
        public void setModeConsistent(boolean modeConsistent) { this.modeConsistent = modeConsistent; }

        public ModeType getSuggestedMode() { return suggestedMode; }
        public void setSuggestedMode(ModeType suggestedMode) { this.suggestedMode = suggestedMode; }
    }

    /**
     * 心跳状态枚举
     */
    public enum HeartbeatStatus {
        HEALTHY,      // 健康
        MISSING,      // 错过心跳
        TIMEOUT,      // 超时
        INACTIVE,     // 非活跃
        NOT_FOUND     // 未找到
    }

    /**
     * 心跳统计
     */
    public static class HeartbeatStats {
        private int totalClients;
        private int activeClients;
        private int healthyClients;
        private double healthRate;
        private long totalHeartbeats;
        private long missedHeartbeats;
        private long lastUpdateTime;

        // Getters and Setters
        public int getTotalClients() { return totalClients; }
        public void setTotalClients(int totalClients) { this.totalClients = totalClients; }

        public int getActiveClients() { return activeClients; }
        public void setActiveClients(int activeClients) { this.activeClients = activeClients; }

        public int getHealthyClients() { return healthyClients; }
        public void setHealthyClients(int healthyClients) { this.healthyClients = healthyClients; }

        public double getHealthRate() { return healthRate; }
        public void setHealthRate(double healthRate) { this.healthRate = healthRate; }

        public long getTotalHeartbeats() { return totalHeartbeats; }
        public void setTotalHeartbeats(long totalHeartbeats) { this.totalHeartbeats = totalHeartbeats; }

        public long getMissedHeartbeats() { return missedHeartbeats; }
        public void setMissedHeartbeats(long missedHeartbeats) { this.missedHeartbeats = missedHeartbeats; }

        public long getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }

        @Override
        public String toString() {
            return String.format(
                    "HeartbeatStats{总客户端=%d, 活跃客户端=%d, 健康客户端=%d, " +
                            "健康率=%.2f, 总心跳数=%d, 错失心跳数=%d}",
                    totalClients, activeClients, healthyClients,
                    healthRate, totalHeartbeats, missedHeartbeats
            );
        }
    }
}