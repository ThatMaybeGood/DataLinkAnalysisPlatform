package com.workflow.platform.controller;

import com.workflow.platform.component.HeartbeatManager;
import com.workflow.platform.component.ModeConsistencyChecker;
import com.workflow.platform.component.WebSocketHandler;
import com.workflow.platform.model.dto.*;
import com.workflow.platform.util.OfflineSyncUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coordination")
@Tag(name = "协调管理", description = "前后端协调管理接口")
public class CoordinationController {

    @Autowired
    private HeartbeatManager heartbeatManager;

    @Autowired
    private ModeConsistencyChecker modeConsistencyChecker;

    @Autowired
    private WebSocketHandler webSocketHandler;

    @Autowired
    private OfflineSyncUtil offlineSyncUtil;

    @PostMapping("/heartbeat")
    @Operation(summary = "发送心跳")
    public ResponseEntity<HeartbeatResponse> sendHeartbeat(@RequestBody HeartbeatDTO heartbeat) {
        HeartbeatManager.HeartbeatResponse response =
                heartbeatManager.processHeartbeat(heartbeat);

        // 转换为DTO
        HeartbeatResponse dtoResponse = new HeartbeatResponse();
        dtoResponse.setTimestamp(response.getTimestamp());
        dtoResponse.setServerTime(response.getServerTime());
        dtoResponse.setServerMode(response.getServerMode());
        dtoResponse.setNextHeartbeatInterval(response.getNextHeartbeatInterval());
        dtoResponse.setNeedsSync(response.isNeedsSync());
        dtoResponse.setModeConsistent(response.isModeConsistent());
        dtoResponse.setSuggestedMode(response.getSuggestedMode());

        return ResponseEntity.ok(dtoResponse);
    }

    @GetMapping("/status/{clientId}")
    @Operation(summary = "获取客户端状态")
    public ResponseEntity<ClientStatusDTO> getClientStatus(@PathVariable String clientId) {
        HeartbeatManager.HeartbeatStatus status =
                heartbeatManager.getClientStatus(clientId);

        ModeConsistencyChecker.ClientModeInfo modeInfo =
                modeConsistencyChecker.getClientModeInfo(clientId);

        ClientStatusDTO dto = new ClientStatusDTO();
        dto.setClientId(clientId);
        dto.setHeartbeatStatus(status.name());
        dto.setOnline(status == HeartbeatManager.HeartbeatStatus.HEALTHY);
        dto.setLastHeartbeatTime(modeInfo != null ? modeInfo.getLastHeartbeat() : 0);
        dto.setModeConsistent(modeInfo != null ? modeInfo.isConsistent() : false);

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/status/all")
    @Operation(summary = "获取所有客户端状态")
    public ResponseEntity<Map<String, String>> getAllClientStatus() {
        Map<String, HeartbeatManager.HeartbeatStatus> statusMap =
                heartbeatManager.getAllClientStatus();

        Map<String, String> result = new java.util.HashMap<>();
        for (Map.Entry<String, HeartbeatManager.HeartbeatStatus> entry : statusMap.entrySet()) {
            result.put(entry.getKey(), entry.getValue().name());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/mode/consistency")
    @Operation(summary = "检查模式一致性")
    public ResponseEntity<ModeConsistencyReportDTO> checkModeConsistency() {
        ModeConsistencyChecker.ConsistencyReport report =
                modeConsistencyChecker.checkAllClients();

        ModeConsistencyReportDTO dto = new ModeConsistencyReportDTO();
        dto.setCheckTime(report.getCheckTime());
        dto.setServerMode(report.getServerMode());
        dto.setTotalClients(report.getTotalClients());
        dto.setConsistentClients(report.getConsistentClients());
        dto.setInconsistentClients(report.getInconsistentClients());
        dto.setStaleClients(report.getStaleClients());
        dto.setConsistencyRate(report.getConsistencyRate());

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/mode/sync")
    @Operation(summary = "同步客户端模式")
    public ResponseEntity<Map<String, Object>> syncClientMode(
            @RequestParam String clientId,
            @RequestParam String mode) {

        boolean synced = modeConsistencyChecker.forceSyncClientMode(
                clientId, com.workflow.platform.enums.ModeType.fromCode(mode));

        if (synced) {
            // 发送WebSocket通知
            webSocketHandler.sendModeSwitchNotification(mode, "管理员强制同步");
        }

        return ResponseEntity.ok(Map.of(
                "success", synced,
                "message", synced ? "模式同步成功" : "模式同步失败"
        ));
    }

    @GetMapping("/system/status")
    @Operation(summary = "获取系统状态")
    public ResponseEntity<SystemStatusDTO> getSystemStatus() {
        SystemStatusDTO status = new SystemStatusDTO();

        status.setServerTime(System.currentTimeMillis());
        status.setServerMode(com.workflow.platform.enums.ModeType.ONLINE);
        status.setServerVersion("1.0.0");
        status.setSystemStatus("healthy");

        // 获取统计信息
        HeartbeatManager.HeartbeatStats heartbeatStats = heartbeatManager.getStats();
        ModeConsistencyChecker.ModeConsistencyStats modeStats =
                modeConsistencyChecker.getStats();
        OfflineSyncUtil.SyncStatus syncStatus = offlineSyncUtil.getSyncStatus();

        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("activeClients", heartbeatStats.getActiveClients());
        stats.put("totalHeartbeats", heartbeatStats.getTotalHeartbeats());
        stats.put("modeConsistencyRate", modeStats.getConsistencyRate());
        stats.put("syncQueueSize", syncStatus.getQueueSize());
        stats.put("pendingSyncTasks", syncStatus.getPendingTasks());

        status.setStatistics(stats);

        return ResponseEntity.ok(status);
    }

    @PostMapping("/notification/broadcast")
    @Operation(summary = "广播通知")
    public ResponseEntity<Map<String, Object>> broadcastNotification(
            @RequestBody NotificationDTO notification) {

        WebSocketMessage message = new WebSocketMessage();
        message.setType("notification");
        message.setTimestamp(System.currentTimeMillis());
        message.setData(Map.of(
                "title", notification.getTitle(),
                "content", notification.getContent(),
                "level", notification.getLevel(),
                "type", notification.getType(),
                "sender", "system"
        ));

        if ("all".equals(notification.getTarget())) {
            webSocketHandler.broadcast(message);
        } else if ("mode".equals(notification.getTarget())) {
            webSocketHandler.broadcastToMode(notification.getTargetValue(), message);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "通知已发送"
        ));
    }

    @GetMapping("/clients/active")
    @Operation(summary = "获取活跃客户端列表")
    public ResponseEntity<List<String>> getActiveClients() {
        List<String> activeClients = heartbeatManager.getActiveClients();
        return ResponseEntity.ok(activeClients);
    }

    @PostMapping("/client/{clientId}/offline")
    @Operation(summary = "强制客户端离线")
    public ResponseEntity<Map<String, Object>> forceClientOffline(@PathVariable String clientId) {
        boolean success = heartbeatManager.forceClientOffline(clientId);

        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "客户端已强制离线" : "操作失败"
        ));
    }

    @GetMapping("/stats")
    @Operation(summary = "获取协调统计")
    public ResponseEntity<CoordinationStatsDTO> getCoordinationStats() {
        CoordinationStatsDTO stats = new CoordinationStatsDTO();

        // 心跳统计
        HeartbeatManager.HeartbeatStats heartbeatStats = heartbeatManager.getStats();
        stats.setTotalClients(heartbeatStats.getTotalClients());
        stats.setActiveClients(heartbeatStats.getActiveClients());
        stats.setHealthyClients(heartbeatStats.getHealthyClients());
        stats.setHeartbeatHealthRate(heartbeatStats.getHealthRate());

        // 模式一致性统计
        ModeConsistencyChecker.ModeConsistencyStats modeStats =
                modeConsistencyChecker.getStats();
        stats.setModeConsistencyRate(modeStats.getConsistencyRate());
        stats.setInconsistencyCount(modeStats.getInconsistencyCount());

        // 同步统计
        OfflineSyncUtil.SyncStatus syncStatus = offlineSyncUtil.getSyncStatus();
        stats.setSyncQueueSize(syncStatus.getQueueSize());
        stats.setPendingSyncTasks(syncStatus.getPendingTasks());
        stats.setSuccessSyncTasks(syncStatus.getSuccessTasks());
        stats.setFailedSyncTasks(syncStatus.getFailedTasks());

        // WebSocket统计
        stats.setWebSocketConnections(getWebSocketConnectionCount());

        stats.setLastUpdateTime(System.currentTimeMillis());

        return ResponseEntity.ok(stats);
    }

    @PostMapping("/sync/trigger")
    @Operation(summary = "触发数据同步")
    public ResponseEntity<Map<String, Object>> triggerDataSync(
            @RequestParam String clientId,
            @RequestParam String syncType) {

        // 准备同步数据
        Map<String, Object> syncData = Map.of(
                "type", syncType,
                "clientId", clientId,
                "triggerTime", System.currentTimeMillis()
        );

        // 发送WebSocket同步通知
        webSocketHandler.sendDataSyncNotification(clientId, syncType, syncData);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "同步已触发"
        ));
    }

    private int getWebSocketConnectionCount() {
        // 在实际应用中，这里应该从WebSocketHandler获取连接数
        // 这里简化为固定值
        return 10;
    }
}