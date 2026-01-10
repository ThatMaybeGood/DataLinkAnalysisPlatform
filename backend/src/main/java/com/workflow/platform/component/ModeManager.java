package com.workflow.platform.component;
//模式管理器

import com.workflow.platform.enums.ModeType;
import com.workflow.platform.exception.ModeNotAllowedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模式管理器 - 统一管理在线/离线模式
 */
@Slf4j
@Component
public class ModeManager {

    private final AtomicReference<ModeType> currentMode = new AtomicReference<>();
    private final Object lock = new Object();

    public ModeManager() {
        this.currentMode.set(ModeType.ONLINE);
    }

    public ModeManager(ModeType initialMode) {
        this.currentMode.set(initialMode);
    }

    @PostConstruct
    public void init() {
        log.info("初始化模式管理器，当前模式: {}", currentMode.get());
    }

    /**
     * 获取当前模式
     */
    public ModeType getCurrentMode() {
        return currentMode.get();
    }

    /**
     * 切换模式
     */
    public boolean switchMode(ModeType newMode) {
        synchronized (lock) {
            ModeType oldMode = currentMode.get();
            if (oldMode == newMode) {
                log.warn("模式未改变，当前已是: {}", newMode);
                return false;
            }

            try {
                // 执行模式切换前的清理工作
                cleanupBeforeSwitch(oldMode);

                // 更新模式
                currentMode.set(newMode);

                // 执行模式切换后的初始化工作
                initializeAfterSwitch(newMode);

                log.info("模式切换成功: {} -> {}", oldMode, newMode);
                return true;

            } catch (Exception e) {
                log.error("模式切换失败: {} -> {}, 错误: {}", oldMode, newMode, e.getMessage(), e);
                // 回滚到原模式
                currentMode.set(oldMode);
                return false;
            }
        }
    }

    /**
     * 检查是否允许某种模式的操作
     */
    public void checkModeAllowed(ModeType requiredMode) {
        ModeType current = currentMode.get();
        if (current != requiredMode && current != ModeType.MIXED) {
            throw new ModeNotAllowedException(
                    String.format("当前模式[%s]不允许此操作，需要模式[%s]", current, requiredMode)
            );
        }
    }

    /**
     * 检查是否在线模式
     */
    public boolean isOnlineMode() {
        return currentMode.get() == ModeType.ONLINE || currentMode.get() == ModeType.MIXED;
    }

    /**
     * 检查是否离线模式
     */
    public boolean isOfflineMode() {
        return currentMode.get() == ModeType.OFFLINE || currentMode.get() == ModeType.MIXED;
    }

    /**
     * 获取模式描述
     */
    public String getModeDescription() {
        ModeType mode = currentMode.get();
        return String.format("%s(%s)", mode.getDescription(), mode.getCode());
    }

    /**
     * 获取系统状态
     */
    public SystemStatus getSystemStatus() {
        SystemStatus status = new SystemStatus();
        status.setMode(currentMode.get());
        status.setAvailable(true);
        status.setLastUpdate(System.currentTimeMillis());
        return status;
    }

    /**
     * 模式切换前的清理工作
     */
    private void cleanupBeforeSwitch(ModeType oldMode) {
        log.info("开始清理{}模式相关资源", oldMode);
        // 这里可以添加具体的清理逻辑
        // 例如：关闭数据库连接、清理缓存、停止定时任务等
        try {
            Thread.sleep(100); // 模拟清理工作
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 模式切换后的初始化工作
     */
    private void initializeAfterSwitch(ModeType newMode) {
        log.info("开始初始化{}模式", newMode);
        // 这里可以添加具体的初始化逻辑
        // 例如：建立数据库连接、初始化缓存、启动定时任务等
        try {
            Thread.sleep(100); // 模拟初始化工作
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 系统状态内部类
     */
    public static class SystemStatus {
        private ModeType mode;
        private boolean available;
        private long lastUpdate;

        // Getters and Setters
        public ModeType getMode() {
            return mode;
        }

        public void setMode(ModeType mode) {
            this.mode = mode;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public long getLastUpdate() {
            return lastUpdate;
        }

        public void setLastUpdate(long lastUpdate) {
            this.lastUpdate = lastUpdate;
        }
    }
}