package com.workflow.platform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 工作流服务工厂 - 根据模式返回对应的服务实现
 */
@Component
public class WorkflowServiceFactory {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    /**
     * 获取工作流服务
     */
    public WorkflowService getWorkflowService() {
        String mode = getCurrentMode();
        return applicationContext.getBean(mode + "WorkflowService", WorkflowService.class);
    }

    /**
     * 获取节点服务
     */
    public NodeService getNodeService() {
        String mode = getCurrentMode();
        return applicationContext.getBean(mode + "NodeService", NodeService.class);
    }

    /**
     * 获取验证规则服务
     */
    public ValidationService getValidationService() {
        String mode = getCurrentMode();
        return applicationContext.getBean(mode + "ValidationService", ValidationService.class);
    }

    /**
     * 获取连接器服务
     */
    public ConnectorService getConnectorService() {
        String mode = getCurrentMode();
        return applicationContext.getBean(mode + "ConnectorService", ConnectorService.class);
    }

    /**
     * 获取执行服务
     */
    public ExecutionService getExecutionService() {
        String mode = getCurrentMode();
        return applicationContext.getBean(mode + "ExecutionService", ExecutionService.class);
    }

    /**
     * 获取文件服务
     */
    public FileService getFileService() {
        String mode = getCurrentMode();
        return applicationContext.getBean(mode + "FileService", FileService.class);
    }

    /**
     * 获取当前模式
     */
    private String getCurrentMode() {
        String mode = environment.getProperty("app.mode", "online");
        if (!"online".equals(mode) && !"offline".equals(mode)) {
            throw new IllegalStateException("无效的应用模式: " + mode);
        }
        return mode;
    }

    /**
     * 检查当前是否为离线模式
     */
    public boolean isOfflineMode() {
        return "offline".equals(getCurrentMode());
    }

    /**
     * 检查当前是否为在线模式
     */
    public boolean isOnlineMode() {
        return "online".equals(getCurrentMode());
    }
}