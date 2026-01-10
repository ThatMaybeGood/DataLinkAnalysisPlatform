package com.workflow.platform.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.platform.model.dto.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket处理器 - 处理实时通信
 */
@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HeartbeatManager heartbeatManager;

    @Autowired
    private ModeConsistencyChecker modeConsistencyChecker;

    // 客户端会话管理
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionClientMap = new ConcurrentHashMap<>();

    // 调度器
    private ScheduledExecutorService pingScheduler;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);

        log.info("WebSocket连接建立: {}", sessionId);

        // 发送欢迎消息
        sendWelcomeMessage(session);

        // 启动ping任务（如果是首次连接）
        if (pingScheduler == null) {
            startPingTask();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();

        try {
            // 解析消息
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);

            // 处理不同类型的消息
            switch (wsMessage.getType()) {
                case "heartbeat":
                    handleHeartbeatMessage(session, wsMessage);
                    break;
                case "mode_update":
                    handleModeUpdateMessage(session, wsMessage);
                    break;
                case "sync_request":
                    handleSyncRequestMessage(session, wsMessage);
                    break;
                case "data_update":
                    handleDataUpdateMessage(session, wsMessage);
                    break;
                case "notification":
                    handleNotificationMessage(session, wsMessage);
                    break;
                case "ping":
                    handlePingMessage(session, wsMessage);
                    break;
                default:
                    log.warn("未知的WebSocket消息类型: {}", wsMessage.getType());
                    sendErrorMessage(session, "未知消息类型", wsMessage);
            }

        } catch (Exception e) {
            log.error("处理WebSocket消息失败: {}，错误: {}", payload, e.getMessage(), e);
            sendErrorMessage(session, "消息处理失败: " + e.getMessage(), null);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);

        String clientId = sessionClientMap.remove(sessionId);
        if (clientId != null) {
            log.info("WebSocket连接关闭: {}，客户端: {}，状态: {}",
                    sessionId, clientId, status);

            // 通知客户端离线
            notifyClientOffline(clientId);
        } else {
            log.info("WebSocket连接关闭: {}，状态: {}", sessionId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("WebSocket传输错误: {}，错误: {}", sessionId, exception.getMessage(), exception);
    }

    /**
     * 发送消息到特定客户端
     */
    public void sendToClient(String clientId, WebSocketMessage message) {
        String sessionId = findSessionIdByClientId(clientId);
        if (sessionId != null) {
            sendToSession(sessionId, message);
        } else {
            log.warn("客户端 {} 未找到活跃的WebSocket会话", clientId);
        }
    }

    /**
     * 广播消息到所有客户端
     */
    public void broadcast(WebSocketMessage message) {
        for (String sessionId : sessions.keySet()) {
            sendToSession(sessionId, message);
        }
    }

    /**
     * 广播消息到特定模式的客户端
     */
    public void broadcastToMode(String mode, WebSocketMessage message) {
        for (Map.Entry<String, String> entry : sessionClientMap.entrySet()) {
            String sessionId = entry.getKey();
            String clientId = entry.getValue();

            // 获取客户端模式信息
            ModeConsistencyChecker.ClientModeInfo info =
                    modeConsistencyChecker.getClientModeInfo(clientId);

            if (info != null && mode.equals(info.getClientMode().getCode())) {
                sendToSession(sessionId, message);
            }
        }
    }

    /**
     * 发送系统状态更新
     */
    public void sendSystemStatusUpdate() {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("system_status");
        message.setTimestamp(System.currentTimeMillis());
        message.setData(Map.of("action", "update", "time", System.currentTimeMillis()));

        broadcast(message);
    }

    /**
     * 发送模式切换通知
     */
    public void sendModeSwitchNotification(String newMode, String reason) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("mode_switch");
        message.setTimestamp(System.currentTimeMillis());
        message.setData(Map.of(
                "newMode", newMode,
                "reason", reason,
                "effectiveTime", System.currentTimeMillis()
        ));

        broadcast(message);
        log.info("发送模式切换通知: {}，原因: {}", newMode, reason);
    }

    /**
     * 发送数据同步通知
     */
    public void sendDataSyncNotification(String clientId, String syncType, Map<String, Object> data) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("data_sync");
        message.setTimestamp(System.currentTimeMillis());
        message.setData(Map.of(
                "syncType", syncType,
                "data", data,
                "clientId", clientId
        ));

        sendToClient(clientId, message);
    }

    // ========== 私有方法 ==========

    private void handleHeartbeatMessage(WebSocketSession session, WebSocketMessage message) {
        String sessionId = session.getId();
        Map<String, Object> data = (Map<String, Object>) message.getData();

        String clientId = (String) data.get("clientId");
        String mode = (String) data.get("mode");
        String version = (String) data.get("version");
        String platform = (String) data.get("platform");

        // 更新会话-客户端映射
        sessionClientMap.put(sessionId, clientId);

        // 创建心跳DTO
        com.workflow.platform.model.dto.HeartbeatDTO heartbeat =
                new com.workflow.platform.model.dto.HeartbeatDTO();
        heartbeat.setClientId(clientId);
        heartbeat.setMode(com.workflow.platform.enums.ModeType.fromCode(mode));
        heartbeat.setClientVersion(version);
        heartbeat.setPlatform(platform);
        heartbeat.setSessionId(sessionId);
        heartbeat.setTimestamp(message.getTimestamp());

        // 处理心跳
        HeartbeatManager.HeartbeatResponse response =
                heartbeatManager.processHeartbeat(heartbeat);

        // 发送心跳响应
        WebSocketMessage responseMessage = new WebSocketMessage();
        responseMessage.setType("heartbeat_response");
        responseMessage.setTimestamp(System.currentTimeMillis());
        responseMessage.setData(Map.of(
                "serverTime", response.getServerTime(),
                "serverMode", response.getServerMode().getCode(),
                "nextInterval", response.getNextHeartbeatInterval(),
                "needsSync", response.isNeedsSync(),
                "modeConsistent", response.isModeConsistent(),
                "suggestedMode", response.getSuggestedMode() != null ?
                        response.getSuggestedMode().getCode() : null
        ));

        sendToSession(sessionId, responseMessage);
    }

    private void handleModeUpdateMessage(WebSocketSession session, WebSocketMessage message) {
        String sessionId = session.getId();
        Map<String, Object> data = (Map<String, Object>) message.getData();

        String clientId = (String) data.get("clientId");
        String newMode = (String) data.get("newMode");
        String reason = (String) data.get("reason");

        log.info("客户端 {} 请求切换模式: {}，原因: {}", clientId, newMode, reason);

        // 强制同步客户端模式
        boolean synced = modeConsistencyChecker.forceSyncClientMode(
                clientId, com.workflow.platform.enums.ModeType.fromCode(newMode));

        // 发送响应
        WebSocketMessage response = new WebSocketMessage();
        response.setType("mode_update_response");
        response.setTimestamp(System.currentTimeMillis());
        response.setData(Map.of(
                "success", synced,
                "newMode", newMode,
                "message", synced ? "模式已更新" : "模式更新失败"
        ));

        sendToSession(sessionId, response);

        // 如果成功，广播模式更新通知
        if (synced) {
            sendModeSwitchNotification(newMode, "客户端请求: " + reason);
        }
    }

    private void handleSyncRequestMessage(WebSocketSession session, WebSocketMessage message) {
        String sessionId = session.getId();
        Map<String, Object> data = (Map<String, Object>) message.getData();

        String clientId = (String) data.get("clientId");
        String syncType = (String) data.get("syncType");
        Map<String, Object> syncData = (Map<String, Object>) data.get("data");

        log.info("客户端 {} 请求数据同步，类型: {}", clientId, syncType);

        // 处理同步请求
        // 在实际应用中，这里应该调用具体的同步服务

        // 发送同步响应
        WebSocketMessage response = new WebSocketMessage();
        response.setType("sync_response");
        response.setTimestamp(System.currentTimeMillis());
        response.setData(Map.of(
                "syncType", syncType,
                "status", "processing",
                "message", "同步请求已接收，正在处理"
        ));

        sendToSession(sessionId, response);
    }

    private void handleDataUpdateMessage(WebSocketSession session, WebSocketMessage message) {
        String sessionId = session.getId();
        Map<String, Object> data = (Map<String, Object>) message.getData();

        String updateType = (String) data.get("updateType");
        Object updateData = data.get("data");

        log.debug("收到数据更新消息，类型: {}", updateType);

        // 处理数据更新
        // 在实际应用中，这里应该更新相应的服务

        // 广播更新通知（如果需要）
        if (Boolean.TRUE.equals(data.get("broadcast"))) {
            WebSocketMessage broadcastMessage = new WebSocketMessage();
            broadcastMessage.setType("data_update_broadcast");
            broadcastMessage.setTimestamp(System.currentTimeMillis());
            broadcastMessage.setData(Map.of(
                    "updateType", updateType,
                    "data", updateData,
                    "sourceSession", sessionId
            ));

            // 排除发送者
            broadcastExcluding(sessionId, broadcastMessage);
        }
    }

    private void handleNotificationMessage(WebSocketSession session, WebSocketMessage message) {
        String sessionId = session.getId();
        Map<String, Object> data = (Map<String, Object>) message.getData();

        String notificationType = (String) data.get("type");
        String content = (String) data.get("content");

        log.info("收到通知消息，类型: {}，内容: {}", notificationType, content);

        // 处理通知
        // 在实际应用中，这里应该记录通知或触发相应操作
    }

    private void handlePingMessage(WebSocketSession session, WebSocketMessage message) {
        String sessionId = session.getId();

        // 发送pong响应
        WebSocketMessage pong = new WebSocketMessage();
        pong.setType("pong");
        pong.setTimestamp(System.currentTimeMillis());
        pong.setData(Map.of("receivedAt", message.getTimestamp()));

        sendToSession(sessionId, pong);
    }

    private void sendWelcomeMessage(WebSocketSession session) throws IOException {
        WebSocketMessage welcome = new WebSocketMessage();
        welcome.setType("welcome");
        welcome.setTimestamp(System.currentTimeMillis());
        welcome.setData(Map.of(
                "message", "欢迎连接到工作流平台",
                "serverTime", System.currentTimeMillis(),
                "supportedFeatures", new String[]{"heartbeat", "sync", "notifications"}
        ));

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(welcome)));
    }

    private void sendErrorMessage(WebSocketSession session, String error, WebSocketMessage original)
            throws IOException {
        WebSocketMessage errorMsg = new WebSocketMessage();
        errorMsg.setType("error");
        errorMsg.setTimestamp(System.currentTimeMillis());
        errorMsg.setData(Map.of(
                "error", error,
                "originalType", original != null ? original.getType() : null,
                "originalTimestamp", original != null ? original.getTimestamp() : null
        ));

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorMsg)));
    }

    private void sendToSession(String sessionId, WebSocketMessage message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("发送WebSocket消息失败: {}，错误: {}", sessionId, e.getMessage(), e);
                // 清理无效会话
                sessions.remove(sessionId);
                sessionClientMap.remove(sessionId);
            }
        } else {
            // 清理无效会话
            sessions.remove(sessionId);
            sessionClientMap.remove(sessionId);
        }
    }

    private void broadcastExcluding(String excludedSessionId, WebSocketMessage message) {
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String sessionId = entry.getKey();
            if (!sessionId.equals(excludedSessionId)) {
                sendToSession(sessionId, message);
            }
        }
    }

    private String findSessionIdByClientId(String clientId) {
        for (Map.Entry<String, String> entry : sessionClientMap.entrySet()) {
            if (clientId.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void notifyClientOffline(String clientId) {
        // 发送客户端离线通知
        WebSocketMessage offlineMsg = new WebSocketMessage();
        offlineMsg.setType("client_offline");
        offlineMsg.setTimestamp(System.currentTimeMillis());
        offlineMsg.setData(Map.of(
                "clientId", clientId,
                "offlineTime", System.currentTimeMillis()
        ));

        broadcastExcluding(findSessionIdByClientId(clientId), offlineMsg);
    }

    private void startPingTask() {
        pingScheduler = Executors.newScheduledThreadPool(1);

        pingScheduler.scheduleAtFixedRate(() -> {
            try {
                sendPingToAll();
            } catch (Exception e) {
                log.error("发送ping任务异常: {}", e.getMessage(), e);
            }
        }, 30, 30, TimeUnit.SECONDS);

        log.info("WebSocket ping任务已启动，间隔30秒");
    }

    private void sendPingToAll() {
        WebSocketMessage ping = new WebSocketMessage();
        ping.setType("ping");
        ping.setTimestamp(System.currentTimeMillis());
        ping.setData(Map.of("serverTime", System.currentTimeMillis()));

        for (String sessionId : sessions.keySet()) {
            sendToSession(sessionId, ping);
        }
    }
}