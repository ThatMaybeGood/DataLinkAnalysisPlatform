package com.workflow.platform.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模式上下文管理器
 * 用于在整个应用中传递和存储模式相关的上下文信息
 * 使用ThreadLocal保证线程安全，支持嵌套调用
 */
@Component
public class ModeContext {

    /**
     * 模式上下文持有者
     * 使用InheritableThreadLocal支持子线程继承模式信息
     */
    private static final ThreadLocal<ContextData> CONTEXT_HOLDER =
            new InheritableThreadLocal<>();

    /**
     * 设置当前模式
     *
     * @param mode 模式：online/offline
     * @throws IllegalArgumentException 如果模式无效
     */
    public void setMode(String mode) {
        if (!"online".equals(mode) && !"offline".equals(mode)) {
            throw new IllegalArgumentException("无效的模式: " + mode + "，必须是 'online' 或 'offline'");
        }

        ContextData context = CONTEXT_HOLDER.get();
        if (context == null) {
            context = new ContextData();
            CONTEXT_HOLDER.set(context);
        }
        context.setMode(mode);
    }

    /**
     * 获取当前模式
     *
     * @return 当前模式，如果没有设置则返回null
     */
    public String getMode() {
        ContextData context = CONTEXT_HOLDER.get();
        return context != null ? context.getMode() : null;
    }

    /**
     * 检查当前是否为离线模式
     *
     * @return 如果是离线模式返回true，否则返回false
     */
    public boolean isOfflineMode() {
        return "offline".equals(getMode());
    }

    /**
     * 检查当前是否为在线模式
     *
     * @return 如果是在线模式返回true，否则返回false
     */
    public boolean isOnlineMode() {
        return "online".equals(getMode());
    }

    /**
     * 设置当前租户
     *
     * @param tenantId 租户ID
     */
    public void setTenantId(String tenantId) {
        ContextData context = getOrCreateContext();
        context.setTenantId(tenantId);
    }

    /**
     * 获取当前租户
     *
     * @return 当前租户ID
     */
    public String getTenantId() {
        ContextData context = CONTEXT_HOLDER.get();
        return context != null ? context.getTenantId() : null;
    }

    /**
     * 设置当前用户
     *
     * @param userId 用户ID
     */
    public void setUserId(String userId) {
        ContextData context = getOrCreateContext();
        context.setUserId(userId);
    }

    /**
     * 获取当前用户
     *
     * @return 当前用户ID
     */
    public String getUserId() {
        ContextData context = CONTEXT_HOLDER.get();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 设置自定义属性
     *
     * @param key 属性键
     * @param value 属性值
     */
    public void setAttribute(String key, Object value) {
        ContextData context = getOrCreateContext();
        context.setAttribute(key, value);
    }

    /**
     * 获取自定义属性
     *
     * @param key 属性键
     * @return 属性值，如果不存在则返回null
     */
    public Object getAttribute(String key) {
        ContextData context = CONTEXT_HOLDER.get();
        return context != null ? context.getAttribute(key) : null;
    }

    /**
     * 移除自定义属性
     *
     * @param key 属性键
     */
    public void removeAttribute(String key) {
        ContextData context = CONTEXT_HOLDER.get();
        if (context != null) {
            context.removeAttribute(key);
        }
    }

    /**
     * 设置离线模式上下文
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     */
    public void setOfflineContext(String tenantId, String userId) {
        setMode("offline");
        setTenantId(tenantId);
        setUserId(userId);
    }

    /**
     * 设置在线模式上下文
     *
     * @param tenantId 租户ID
     * @param userId 用户ID
     */
    public void setOnlineContext(String tenantId, String userId) {
        setMode("online");
        setTenantId(tenantId);
        setUserId(userId);
    }

    /**
     * 获取完整的上下文信息
     *
     * @return 上下文信息对象
     */
    public ContextInfo getContextInfo() {
        ContextData context = CONTEXT_HOLDER.get();
        if (context == null) {
            return new ContextInfo();
        }

        return ContextInfo.builder()
                .mode(context.getMode())
                .tenantId(context.getTenantId())
                .userId(context.getUserId())
                .attributes(new ConcurrentHashMap<>(context.getAttributes()))
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 清理当前线程的上下文
     */
    public void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 执行带有特定模式的代码块
     *
     * @param mode 执行模式
     * @param action 要执行的代码块
     * @param <T> 返回值类型
     * @return 执行结果
     */
    public <T> T executeWithMode(String mode, ModeAction<T> action) {
        String previousMode = getMode();
        try {
            setMode(mode);
            return action.execute();
        } finally {
            setMode(previousMode);
        }
    }

    /**
     * 执行带有特定上下文的代码块
     *
     * @param context 上下文信息
     * @param action 要执行的代码块
     * @param <T> 返回值类型
     * @return 执行结果
     */
    public <T> T executeWithContext(ContextInfo context, ModeAction<T> action) {
        ContextInfo previousContext = getContextInfo();
        try {
            // 设置新上下文
            if (context.getMode() != null) {
                setMode(context.getMode());
            }
            if (context.getTenantId() != null) {
                setTenantId(context.getTenantId());
            }
            if (context.getUserId() != null) {
                setUserId(context.getUserId());
            }
            if (context.getAttributes() != null) {
                context.getAttributes().forEach(this::setAttribute);
            }

            return action.execute();
        } finally {
            // 恢复原上下文
            clear();
            if (previousContext.getMode() != null) {
                setMode(previousContext.getMode());
            }
            if (previousContext.getTenantId() != null) {
                setTenantId(previousContext.getTenantId());
            }
            if (previousContext.getUserId() != null) {
                setUserId(previousContext.getUserId());
            }
            if (previousContext.getAttributes() != null) {
                previousContext.getAttributes().forEach(this::setAttribute);
            }
        }
    }

    /**
     * 获取或创建上下文数据
     */
    private ContextData getOrCreateContext() {
        ContextData context = CONTEXT_HOLDER.get();
        if (context == null) {
            context = new ContextData();
            CONTEXT_HOLDER.set(context);
        }
        return context;
    }

    // ========== 内部类 ==========

    /**
     * 上下文数据类
     */
    private static class ContextData {
        private String mode;
        private String tenantId;
        private String userId;
        private Map<String, Object> attributes = new ConcurrentHashMap<>();

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public Map<String, Object> getAttributes() { return attributes; }

        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }

        public Object getAttribute(String key) {
            return attributes.get(key);
        }

        public void removeAttribute(String key) {
            attributes.remove(key);
        }
    }

    /**
     * 上下文信息类（对外暴露）
     */
    @Builder
    @Data
    @AllArgsConstructor // 生成全参构造器
    @NoArgsConstructor
    public static class ContextInfo {
        private String mode;
        private String tenantId;
        private String userId;
        private Map<String, Object> attributes;
        private long timestamp;

    }

    /**
     * 模式执行动作接口
     */
    @FunctionalInterface
    public interface ModeAction<T> {
        T execute();
    }
}