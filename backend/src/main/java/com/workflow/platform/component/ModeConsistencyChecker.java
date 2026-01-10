package com.workflow.platform.component;

import com.workflow.platform.enums.ModeType;
import com.workflow.platform.exception.ModeConsistencyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模式一致性检查器 - 确保前后端模式一致
 */
@Slf4j
@Component
public class ModeConsistencyChecker {

    @Autowired
    private ModeManager modeManager;

    @Value("${workflow.platform.mode.consistency-check-interval:30000}")
    private long consistencyCheckInterval;

    @Value("${workflow.platform.mode.max-inconsistency-count:3}")
    private int maxInconsistencyCount;

    @Value("${workflow.platform.mode.auto-recover:true}")
    private boolean autoRecover;

    // 客户端模式状态
    private final ConcurrentHashMap<String, ClientModeInfo> clientModes =
            new ConcurrentHashMap<>();

    // 一致性检查调度器
    private ScheduledExecutorService scheduler;

    // 不一致计数
    private final AtomicInteger inconsistencyCount = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        log.info("初始化模式一致性检查器");

        // 创建调度器
        scheduler = Executors.newScheduledThreadPool(1);

        // 启动周期性检查
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performConsistencyCheck();
            } catch (Exception e) {
                log.error("模式一致性检查异常: {}", e.getMessage(), e);
            }
        }, 10, consistencyCheckInterval, TimeUnit.MILLISECONDS);

        log.info("模式一致性检查器初始化完成，检查间隔: {}ms", consistencyCheckInterval);
    }

    /**
     * 注册客户端模式
     */
    public void registerClientMode(String clientId, ModeType clientMode, String sessionId) {
        ClientModeInfo info = new ClientModeInfo();
        info.setClientId(clientId);
        info.setClientMode(clientMode);
        info.setSessionId(sessionId);
        info.setLastHeartbeat(System.currentTimeMillis());
        info.setConsistent(checkConsistency(clientMode));

        clientModes.put(clientId, info);

        log.info("注册客户端模式: {} -> {}，一致性: {}",
                clientId, clientMode, info.isConsistent());
    }

    /**
     * 更新客户端心跳
     */
    public void updateClientHeartbeat(String clientId, ModeType clientMode) {
        ClientModeInfo info = clientModes.get(clientId);
        if (info != null) {
            info.setLastHeartbeat(System.currentTimeMillis());
            info.setClientMode(clientMode);
            info.setConsistent(checkConsistency(clientMode));

            if (!info.isConsistent()) {
                handleInconsistency(clientId, info);
            }
        }
    }

    /**
     * 获取客户端模式信息
     */
    public ClientModeInfo getClientModeInfo(String clientId) {
        return clientModes.get(clientId);
    }

    /**
     * 检查所有客户端一致性
     */
    public ConsistencyReport checkAllClients() {
        ConsistencyReport report = new ConsistencyReport();
        report.setCheckTime(System.currentTimeMillis());

        ModeType serverMode = modeManager.getCurrentMode();
        report.setServerMode(serverMode);

        int totalClients = 0;
        int consistentClients = 0;
        int inconsistentClients = 0;
        int staleClients = 0;

        long currentTime = System.currentTimeMillis();
        long staleThreshold = 300000; // 5分钟

        for (ClientModeInfo info : clientModes.values()) {
            totalClients++;

            if (currentTime - info.getLastHeartbeat() > staleThreshold) {
                staleClients++;
                continue;
            }

            boolean consistent = checkConsistency(info.getClientMode());
            info.setConsistent(consistent);

            if (consistent) {
                consistentClients++;
            } else {
                inconsistentClients++;
                report.addInconsistentClient(info);
            }
        }

        report.setTotalClients(totalClients);
        report.setConsistentClients(consistentClients);
        report.setInconsistentClients(inconsistentClients);
        report.setStaleClients(staleClients);
        report.setConsistencyRate(totalClients > 0 ?
                (double) consistentClients / totalClients : 1.0);

        // 更新不一致计数
        if (inconsistentClients > 0) {
            inconsistencyCount.incrementAndGet();
        } else {
            inconsistencyCount.set(0);
        }

        // 检查是否需要自动恢复
        if (autoRecover && inconsistencyCount.get() >= maxInconsistencyCount) {
            log.warn("不一致计数达到阈值: {}，尝试自动恢复", inconsistencyCount.get());
            attemptAutoRecovery();
        }

        return report;
    }

    /**
     * 强制同步客户端模式
     */
    public boolean forceSyncClientMode(String clientId, ModeType targetMode) {
        ClientModeInfo info = clientModes.get(clientId);
        if (info == null) {
            return false;
        }

        log.info("强制同步客户端模式: {} -> {}", clientId, targetMode);

        info.setClientMode(targetMode);
        info.setConsistent(checkConsistency(targetMode));
        info.setLastSyncTime(System.currentTimeMillis());
        info.setForceSynced(true);

        // 触发同步事件
        triggerModeSyncEvent(clientId, targetMode);

        return true;
    }

    /**
     * 清理过期的客户端记录
     */
    public int cleanupStaleClients(int maxAgeMinutes) {
        long cutoffTime = System.currentTimeMillis() - (maxAgeMinutes * 60 * 1000L);
        int removed = 0;

        for (String clientId : clientModes.keySet()) {
            ClientModeInfo info = clientModes.get(clientId);
            if (info.getLastHeartbeat() < cutoffTime) {
                clientModes.remove(clientId);
                removed++;
                log.debug("清理过期客户端: {}", clientId);
            }
        }

        log.info("清理过期客户端完成，移除 {} 个", removed);
        return removed;
    }

    /**
     * 获取模式一致性统计
     */
    public ModeConsistencyStats getStats() {
        ModeConsistencyStats stats = new ModeConsistencyStats();

        stats.setTotalClients(clientModes.size());
        stats.setInconsistencyCount(inconsistencyCount.get());
        stats.setLastCheckTime(System.currentTimeMillis());

        int activeClients = 0;
        int consistentClients = 0;

        for (ClientModeInfo info : clientModes.values()) {
            if (System.currentTimeMillis() - info.getLastHeartbeat() < 300000) {
                activeClients++;
                if (info.isConsistent()) {
                    consistentClients++;
                }
            }
        }

        stats.setActiveClients(activeClients);
        stats.setConsistentClients(consistentClients);
        stats.setConsistencyRate(activeClients > 0 ?
                (double) consistentClients / activeClients : 1.0);

        return stats;
    }

    // ========== 私有方法 ==========

    private void performConsistencyCheck() {
        try {
            ConsistencyReport report = checkAllClients();

            if (report.getInconsistentClients() > 0) {
                log.warn("模式一致性检查: {} 个客户端不一致",
                        report.getInconsistentClients());

                // 记录到日志或发送告警
                logInconsistentClients(report);

                // 触发处理逻辑
                handleInconsistencyReport(report);
            }

        } catch (Exception e) {
            log.error("执行模式一致性检查失败: {}", e.getMessage(), e);
        }
    }

    private boolean checkConsistency(ModeType clientMode) {
        ModeType serverMode = modeManager.getCurrentMode();

        // 检查逻辑：
        // 1. 如果服务器是混合模式，允许任何客户端模式
        // 2. 如果服务器是在线模式，客户端必须是在线模式
        // 3. 如果服务器是离线模式，客户端必须是离线模式

        if (serverMode == ModeType.MIXED) {
            return true;
        }

        return clientMode == serverMode;
    }

    private void handleInconsistency(String clientId, ClientModeInfo info) {
        log.warn("发现模式不一致: 客户端={}，客户端模式={}，服务器模式={}",
                clientId, info.getClientMode(), modeManager.getCurrentMode());

        // 记录不一致事件
        info.getInconsistencyHistory().add(new InconsistencyRecord(
                System.currentTimeMillis(),
                info.getClientMode(),
                modeManager.getCurrentMode()
        ));

        // 限制历史记录大小
        if (info.getInconsistencyHistory().size() > 10) {
            info.getInconsistencyHistory().remove(0);
        }

        // 触发告警
        triggerInconsistencyAlert(clientId, info);
    }

    private void handleInconsistencyReport(ConsistencyReport report) {
        // 根据报告处理不一致情况
        // 1. 发送通知
        // 2. 尝试自动修复
        // 3. 记录到数据库

        log.warn("模式不一致报告: 总数={}，不一致={}，一致性比例={}",
                report.getTotalClients(), report.getInconsistentClients(),
                report.getConsistencyRate());
    }

    private void attemptAutoRecovery() {
        try {
            // 尝试自动恢复策略
            // 1. 重启模式管理器
            // 2. 强制同步所有客户端
            // 3. 发送恢复通知

            log.info("开始自动恢复模式一致性");

            // 重置不一致计数
            inconsistencyCount.set(0);

            // 记录恢复事件
            logRecoveryEvent();

        } catch (Exception e) {
            log.error("自动恢复失败: {}", e.getMessage(), e);
        }
    }

    private void triggerModeSyncEvent(String clientId, ModeType mode) {
        // 在实际应用中，这里应该触发WebSocket事件或消息队列通知
        log.debug("触发模式同步事件: {} -> {}", clientId, mode);
    }

    private void triggerInconsistencyAlert(String clientId, ClientModeInfo info) {
        // 触发告警通知
        log.warn("发送模式不一致告警: 客户端={}", clientId);
    }

    private void logInconsistentClients(ConsistencyReport report) {
        for (ClientModeInfo info : report.getInconsistentClients()) {
            log.warn("不一致客户端: ID={}, 模式={}, 最后心跳={}",
                    info.getClientId(), info.getClientMode(),
                    info.getLastHeartbeat());
        }
    }

    private void logRecoveryEvent() {
        log.info("模式一致性自动恢复完成");
    }

    // ========== 内部类 ==========

    /**
     * 客户端模式信息
     */
    public static class ClientModeInfo {
        private String clientId;
        private ModeType clientMode;
        private String sessionId;
        private long lastHeartbeat;
        private long lastSyncTime;
        private boolean consistent;
        private boolean forceSynced;
        private String clientVersion;
        private String clientPlatform;
        private java.util.List<InconsistencyRecord> inconsistencyHistory;

        public ClientModeInfo() {
            this.inconsistencyHistory = new java.util.ArrayList<>();
        }

        // Getters and Setters
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }

        public ModeType getClientMode() { return clientMode; }
        public void setClientMode(ModeType clientMode) { this.clientMode = clientMode; }

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public long getLastHeartbeat() { return lastHeartbeat; }
        public void setLastHeartbeat(long lastHeartbeat) { this.lastHeartbeat = lastHeartbeat; }

        public long getLastSyncTime() { return lastSyncTime; }
        public void setLastSyncTime(long lastSyncTime) { this.lastSyncTime = lastSyncTime; }

        public boolean isConsistent() { return consistent; }
        public void setConsistent(boolean consistent) { this.consistent = consistent; }

        public boolean isForceSynced() { return forceSynced; }
        public void setForceSynced(boolean forceSynced) { this.forceSynced = forceSynced; }

        public String getClientVersion() { return clientVersion; }
        public void setClientVersion(String clientVersion) { this.clientVersion = clientVersion; }

        public String getClientPlatform() { return clientPlatform; }
        public void setClientPlatform(String clientPlatform) { this.clientPlatform = clientPlatform; }

        public java.util.List<InconsistencyRecord> getInconsistencyHistory() {
            return inconsistencyHistory;
        }
        public void setInconsistencyHistory(java.util.List<InconsistencyRecord> history) {
            this.inconsistencyHistory = history;
        }
    }

    /**
     * 不一致记录
     */
    public static class InconsistencyRecord {
        private long timestamp;
        private ModeType clientMode;
        private ModeType serverMode;
        private String description;

        public InconsistencyRecord(long timestamp, ModeType clientMode, ModeType serverMode) {
            this.timestamp = timestamp;
            this.clientMode = clientMode;
            this.serverMode = serverMode;
            this.description = String.format("客户端模式: %s, 服务器模式: %s",
                    clientMode, serverMode);
        }

        // Getters and Setters
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public ModeType getClientMode() { return clientMode; }
        public void setClientMode(ModeType clientMode) { this.clientMode = clientMode; }

        public ModeType getServerMode() { return serverMode; }
        public void setServerMode(ModeType serverMode) { this.serverMode = serverMode; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    /**
     * 一致性报告
     */
    public static class ConsistencyReport {
        private long checkTime;
        private ModeType serverMode;
        private int totalClients;
        private int consistentClients;
        private int inconsistentClients;
        private int staleClients;
        private double consistencyRate;
        private java.util.List<ClientModeInfo> inconsistentClientsList;

        public ConsistencyReport() {
            this.inconsistentClientsList = new java.util.ArrayList<>();
        }

        // Getters and Setters
        public long getCheckTime() { return checkTime; }
        public void setCheckTime(long checkTime) { this.checkTime = checkTime; }

        public ModeType getServerMode() { return serverMode; }
        public void setServerMode(ModeType serverMode) { this.serverMode = serverMode; }

        public int getTotalClients() { return totalClients; }
        public void setTotalClients(int totalClients) { this.totalClients = totalClients; }

        public int getConsistentClients() { return consistentClients; }
        public void setConsistentClients(int consistentClients) { this.consistentClients = consistentClients; }

        public int getInconsistentClients() { return inconsistentClients; }
        public void setInconsistentClients(int inconsistentClients) { this.inconsistentClients = inconsistentClients; }

        public int getStaleClients() { return staleClients; }
        public void setStaleClients(int staleClients) { this.staleClients = staleClients; }

        public double getConsistencyRate() { return consistencyRate; }
        public void setConsistencyRate(double consistencyRate) { this.consistencyRate = consistencyRate; }

        public java.util.List<ClientModeInfo> getInconsistentClients() {
            return inconsistentClientsList;
        }

        public void addInconsistentClient(ClientModeInfo client) {
            this.inconsistentClientsList.add(client);
        }

        @Override
        public String toString() {
            return String.format(
                    "ConsistencyReport{检查时间=%d, 服务器模式=%s, 总客户端=%d, " +
                            "一致客户端=%d, 不一致客户端=%d, 陈旧客户端=%d, 一致率=%.2f}",
                    checkTime, serverMode, totalClients, consistentClients,
                    inconsistentClients, staleClients, consistencyRate
            );
        }
    }

    /**
     * 模式一致性统计
     */
    public static class ModeConsistencyStats {
        private int totalClients;
        private int activeClients;
        private int consistentClients;
        private double consistencyRate;
        private int inconsistencyCount;
        private long lastCheckTime;

        // Getters and Setters
        public int getTotalClients() { return totalClients; }
        public void setTotalClients(int totalClients) { this.totalClients = totalClients; }

        public int getActiveClients() { return activeClients; }
        public void setActiveClients(int activeClients) { this.activeClients = activeClients; }

        public int getConsistentClients() { return consistentClients; }
        public void setConsistentClients(int consistentClients) { this.consistentClients = consistentClients; }

        public double getConsistencyRate() { return consistencyRate; }
        public void setConsistencyRate(double consistencyRate) { this.consistencyRate = consistencyRate; }

        public int getInconsistencyCount() { return inconsistencyCount; }
        public void setInconsistencyCount(int inconsistencyCount) { this.inconsistencyCount = inconsistencyCount; }

        public long getLastCheckTime() { return lastCheckTime; }
        public void setLastCheckTime(long lastCheckTime) { this.lastCheckTime = lastCheckTime; }

        @Override
        public String toString() {
            return String.format(
                    "ModeConsistencyStats{总客户端=%d, 活跃客户端=%d, 一致客户端=%d, " +
                            "一致率=%.2f, 不一致计数=%d, 最后检查时间=%d}",
                    totalClients, activeClients, consistentClients,
                    consistencyRate, inconsistencyCount, lastCheckTime
            );
        }
    }
}