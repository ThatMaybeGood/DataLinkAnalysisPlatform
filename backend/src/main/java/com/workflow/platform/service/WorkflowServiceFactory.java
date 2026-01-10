package com.workflow.platform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 工作流服务工厂
 * 根据当前模式动态返回对应的服务实现
 * 支持在线模式和离线模式的无缝切换
 */
@Component
public class WorkflowServiceFactory {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    /**
     * 获取工作流服务实例
     * 根据配置的app.mode返回对应的服务实现
     *
     * @return 工作流服务实例
     * @throws IllegalStateException 如果模式配置无效
     */
    public WorkflowService getWorkflowService() {
        String mode = getCurrentMode();

        // 根据模式返回对应的服务Bean
        switch (mode) {
            case "online":
                return applicationContext.getBean("onlineWorkflowServiceImpl", WorkflowService.class);
            case "offline":
                return applicationContext.getBean("offlineWorkflowServiceImpl", WorkflowService.class);
            default:
                throw new IllegalStateException("无效的应用模式: " + mode);
        }
    }

    /**
     * 获取节点服务实例
     */
    public NodeService getNodeService() {
        String mode = getCurrentMode();
        return applicationContext.getBean(mode + "NodeService", NodeService.class);
    }

    /**
     * 获取验证规则服务实例
     */
    public ValidationService getValidationService() {
        String mode = getCurrentMode();
        return applicationContext.getBean(mode + "ValidationService", ValidationService.class);
    }

    /**
     * 获取连接器服务实例
     */
    public ConnectorService getConnectorService() {
        String mode = getCurrentMode();
        return applicationContext.getBean(mode + "ConnectorService", ConnectorService.class);
    }

    /**
     * 获取执行服务实例
     */
    public ExecutionService getExecutionService() {
        String mode = getCurrentMode();
        return applicationContext.getBean(mode + "ExecutionService", ExecutionService.class);
    }

    /**
     * 获取文件服务实例
     */
    public FileService getFileService() {
        String mode = getCurrentMode();
        return applicationContext.getBean(mode + "FileService", FileService.class);
    }

    /**
     * 获取当前应用模式
     *
     * @return 当前模式：online 或 offline
     * @throws IllegalStateException 如果模式配置无效
     */
    public String getCurrentMode() {
        // 从环境变量获取模式配置，默认online
        String mode = environment.getProperty("app.mode", "online");

        // 验证模式有效性
        if (!"online".equals(mode) && !"offline".equals(mode)) {
            throw new IllegalStateException("无效的应用模式: " + mode + "，必须是 'online' 或 'offline'");
        }

        return mode;
    }

    /**
     * 检查当前是否为离线模式
     *
     * @return 如果是离线模式返回true，否则返回false
     */
    public boolean isOfflineMode() {
        return "offline".equals(getCurrentMode());
    }

    /**
     * 检查当前是否为在线模式
     *
     * @return 如果是在线模式返回true，否则返回false
     */
    public boolean isOnlineMode() {
        return "online".equals(getCurrentMode());
    }

    /**
     * 获取模式信息
     *
     * @return 包含模式详细信息的对象
     */
    public ModeInfo getModeInfo() {
        String mode = getCurrentMode();
        ModeInfo info = new ModeInfo();
        info.setMode(mode);
        info.setDescription(getModeDescription(mode));
        info.setFeatures(getModeFeatures(mode));
        info.setStorageType(getStorageType(mode));
        info.setTimestamp(System.currentTimeMillis());
        return info;
    }

    /**
     * 获取模式描述
     */
    private String getModeDescription(String mode) {
        switch (mode) {
            case "online":
                return "在线模式，数据存储在数据库中，支持多用户协作和实时更新";
            case "offline":
                return "离线模式，数据存储在本地文件系统中，支持完全离线工作";
            default:
                return "未知模式";
        }
    }

    /**
     * 获取模式特性
     */
    private String[] getModeFeatures(String mode) {
        switch (mode) {
            case "online":
                return new String[] {
                        "数据库存储",
                        "多用户协作",
                        "实时更新",
                        "权限控制",
                        "高性能查询"
                };
            case "offline":
                return new String[] {
                        "文件系统存储",
                        "完全离线工作",
                        "本地数据安全",
                        "快速启动",
                        "无需网络"
                };
            default:
                return new String[] {};
        }
    }

    /**
     * 获取存储类型
     */
    private String getStorageType(String mode) {
        switch (mode) {
            case "online":
                return environment.getProperty("app.online.database.type", "mysql");
            case "offline":
                return environment.getProperty("app.offline.storage.type", "file");
            default:
                return "unknown";
        }
    }

    /**
     * 模式信息类
     */
    public static class ModeInfo {
        private String mode;
        private String description;
        private String[] features;
        private String storageType;
        private long timestamp;

        // Getters and Setters
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String[] getFeatures() { return features; }
        public void setFeatures(String[] features) { this.features = features; }

        public String getStorageType() { return storageType; }
        public void setStorageType(String storageType) { this.storageType = storageType; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}