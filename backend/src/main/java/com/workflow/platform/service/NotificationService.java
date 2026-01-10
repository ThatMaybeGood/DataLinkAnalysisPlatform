package com.workflow.platform.service;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:43
 */

import java.util.List;
import java.util.Map;

/**
 * 通知服务接口
 */
public interface NotificationService {

    /**
     * 发送冲突通知
     */
    void sendConflictNotification(Map<String, Object> conflictData);

    /**
     * 发送冲突解决通知
     */
    void sendResolutionNotification(Map<String, Object> resolutionData);

    /**
     * 发送系统通知
     */
    void sendSystemNotification(String title, String content, String notificationType,
                                List<String> targetUsers, Map<String, Object> additionalData);

    /**
     * 发送工作流状态变更通知
     */
    void sendWorkflowStatusNotification(String workflowId, String workflowName,
                                        String oldStatus, String newStatus,
                                        String changedBy, Map<String, Object> additionalData);

    /**
     * 发送节点执行通知
     */
    void sendNodeExecutionNotification(String workflowId, String workflowName,
                                       String nodeId, String nodeName,
                                       String executionStatus, String executionResult,
                                       Map<String, Object> additionalData);

    /**
     * 发送同步完成通知
     */
    void sendSyncCompletionNotification(String syncTaskId, String syncType,
                                        String status, String message,
                                        Map<String, Object> syncStats);

    /**
     * 发送离线模式切换通知
     */
    void sendModeSwitchNotification(String oldMode, String newMode,
                                    String switchReason, String initiatedBy,
                                    Map<String, Object> additionalData);

    /**
     * 发送数据备份通知
     */
    void sendBackupNotification(String backupId, String backupType,
                                String status, String message,
                                Map<String, Object> backupStats);

    /**
     * 发送版本创建通知
     */
    void sendVersionCreationNotification(String workflowId, String workflowName,
                                         String versionNumber, String versionName,
                                         String createdBy, Map<String, Object> additionalData);

    /**
     * 发送错误通知
     */
    void sendErrorNotification(String errorCode, String errorMessage,
                               String module, String severity,
                               Map<String, Object> errorData);

    /**
     * 批量发送通知
     */
    void batchSendNotifications(List<Map<String, Object>> notifications);

    /**
     * 获取用户未读通知数量
     */
    int getUnreadNotificationCount(String userId);

    /**
     * 获取用户通知列表
     */
    List<Map<String, Object>> getUserNotifications(String userId, int limit, int offset);

    /**
     * 标记通知为已读
     */
    void markNotificationAsRead(String notificationId, String userId);

    /**
     * 删除通知
     */
    void deleteNotification(String notificationId, String userId);

    /**
     * 清理过期通知
     */
    int cleanupExpiredNotifications(int daysToKeep);
}