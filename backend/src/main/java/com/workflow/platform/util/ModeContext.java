package com.workflow.platform.util;

import org.springframework.stereotype.Component;

/**
 * 模式上下文 - 用于在整个应用中传递模式信息
 */
@Component
public class ModeContext {

    private final ThreadLocal<String> currentMode = ThreadLocal.withInitial(() -> "online");
    private final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    private final ThreadLocal<String> currentUserId = new ThreadLocal<>();

    /**
     * 设置当前模式
     */
    public void setMode(String mode) {
        if ("online".equals(mode) || "offline".equals(mode)) {
            currentMode.set(mode);
        } else {
            throw new IllegalArgumentException("无效的模式: " + mode);
        }
    }

    /**
     * 获取当前模式
     */
    public String getMode() {
        return currentMode.get();
    }

    /**
     * 是否为离线模式
     */
    public boolean isOfflineMode() {
        return "offline".equals(getMode());
    }

    /**
     * 是否为在线模式
     */
    public boolean isOnlineMode() {
        return "online".equals(getMode());
    }

    /**
     * 设置当前租户
     */
    public void setTenant(String tenantId) {
        currentTenant.set(tenantId);
    }

    /**
     * 获取当前租户
     */
    public String getTenant() {
        return currentTenant.get();
    }

    /**
     * 设置当前用户
     */
    public void setUserId(String userId) {
        currentUserId.set(userId);
    }

    /**
     * 获取当前用户
     */
    public String getUserId() {
        return currentUserId.get();
    }

    /**
     * 清理上下文
     */
    public void clear() {
        currentMode.remove();
        currentTenant.remove();
        currentUserId.remove();
    }

    /**
     * 获取模式信息
     */
    public ModeInfo getModeInfo() {
        return ModeInfo.builder()
                .mode(getMode())
                .tenant(getTenant())
                .userId(getUserId())
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建离线模式上下文
     */
    public void setOfflineContext(String tenantId, String userId) {
        setMode("offline");
        setTenant(tenantId);
        setUserId(userId);
    }

    /**
     * 创建在线模式上下文
     */
    public void setOnlineContext(String tenantId, String userId) {
        setMode("online");
        setTenant(tenantId);
        setUserId(userId);
    }

    @lombok.Builder
    @lombok.Data
    public static class ModeInfo {
        private String mode;
        private String tenant;
        private String userId;
        private long timestamp;
    }
}