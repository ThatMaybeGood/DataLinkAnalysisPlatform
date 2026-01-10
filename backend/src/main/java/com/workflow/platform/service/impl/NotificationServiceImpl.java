package com.workflow.platform.service.impl;

import com.workflow.platform.model.entity.NotificationEntity;
import com.workflow.platform.repository.NotificationRepository;
import com.workflow.platform.service.NotificationService;
import com.workflow.platform.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 通知服务实现
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${workflow.notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${workflow.notification.email.from:noreply@workflow-platform.com}")
    private String emailFrom;

    @Value("${workflow.notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${workflow.notification.push.enabled:true}")
    private boolean pushEnabled;

    @Value("${workflow.notification.web.enabled:true}")
    private boolean webEnabled;

    @Value("${workflow.notification.retention.days:30}")
    private int retentionDays;

    @Override
    @Async
    public void sendConflictNotification(Map<String, Object> conflictData) {
        try {
            log.info("发送冲突通知，冲突ID: {}", conflictData.get("conflictId"));

            // 构建通知内容
            String title = "发现数据冲突";
            String content = buildConflictNotificationContent(conflictData);
            String notificationType = "CONFLICT";

            // 获取目标用户（管理员和相关用户）
            List<String> targetUsers = getConflictNotificationTargets(conflictData);

            // 发送Web通知
            if (webEnabled) {
                sendWebNotification(title, content, notificationType, targetUsers, conflictData);
            }

            // 发送邮件通知
            if (emailEnabled && !targetUsers.isEmpty()) {
                sendEmailNotification(title, content, targetUsers, conflictData);
            }

            // 记录通知
            recordNotification(title, content, notificationType, targetUsers, conflictData);

            log.info("冲突通知发送成功，冲突ID: {}", conflictData.get("conflictId"));

        } catch (Exception e) {
            log.error("发送冲突通知失败", e);
        }
    }

    @Override
    @Async
    public void sendResolutionNotification(Map<String, Object> resolutionData) {
        try {
            log.info("发送冲突解决通知，冲突ID: {}", resolutionData.get("conflictId"));

            // 构建通知内容
            String title = "冲突已解决";
            String content = buildResolutionNotificationContent(resolutionData);
            String notificationType = "RESOLUTION";

            // 获取目标用户
            List<String> targetUsers = getResolutionNotificationTargets(resolutionData);

            // 发送Web通知
            if (webEnabled) {
                sendWebNotification(title, content, notificationType, targetUsers, resolutionData);
            }

            // 发送邮件通知
            if (emailEnabled && !targetUsers.isEmpty()) {
                sendEmailNotification(title, content, targetUsers, resolutionData);
            }

            // 记录通知
            recordNotification(title, content, notificationType, targetUsers, resolutionData);

            log.info("冲突解决通知发送成功，冲突ID: {}", resolutionData.get("conflictId"));

        } catch (Exception e) {
            log.error("发送冲突解决通知失败", e);
        }
    }

    @Override
    @Async
    public void sendSystemNotification(String title, String content, String notificationType,
                                       List<String> targetUsers, Map<String, Object> additionalData) {
        try {
            log.info("发送系统通知，标题: {}，类型: {}", title, notificationType);

            // 发送Web通知
            if (webEnabled) {
                sendWebNotification(title, content, notificationType, targetUsers, additionalData);
            }

            // 发送邮件通知（如果需要）
            if (emailEnabled && shouldSendEmail(notificationType) && !targetUsers.isEmpty()) {
                sendEmailNotification(title, content, targetUsers, additionalData);
            }

            // 发送推送通知（如果需要）
            if (pushEnabled && shouldSendPush(notificationType) && !targetUsers.isEmpty()) {
                sendPushNotification(title, content, targetUsers, additionalData);
            }

            // 记录通知
            recordNotification(title, content, notificationType, targetUsers, additionalData);

            log.info("系统通知发送成功，标题: {}", title);

        } catch (Exception e) {
            log.error("发送系统通知失败", e);
        }
    }

    @Override
    @Async
    public void sendWorkflowStatusNotification(String workflowId, String workflowName,
                                               String oldStatus, String newStatus,
                                               String changedBy, Map<String, Object> additionalData) {
        try {
            log.info("发送工作流状态变更通知，工作流ID: {}，状态: {} -> {}",
                    workflowId, oldStatus, newStatus);

            // 构建通知内容
            String title = "工作流状态变更";
            String content = String.format("工作流 '%s' 的状态已从 '%s' 变更为 '%s'，变更人: %s",
                    workflowName, oldStatus, newStatus, changedBy);
            String notificationType = "WORKFLOW_STATUS";

            // 获取目标用户（工作流相关用户）
            List<String> targetUsers = getWorkflowNotificationTargets(workflowId, additionalData);

            // 发送通知
            sendSystemNotification(title, content, notificationType, targetUsers, additionalData);

        } catch (Exception e) {
            log.error("发送工作流状态变更通知失败", e);
        }
    }

    @Override
    @Async
    public void sendNodeExecutionNotification(String workflowId, String workflowName,
                                              String nodeId, String nodeName,
                                              String executionStatus, String executionResult,
                                              Map<String, Object> additionalData) {
        try {
            log.info("发送节点执行通知，工作流ID: {}，节点: {}，状态: {}",
                    workflowId, nodeName, executionStatus);

            // 构建通知内容
            String title = "节点执行结果";
            String content = String.format("工作流 '%s' 的节点 '%s' 执行完成，状态: %s，结果: %s",
                    workflowName, nodeName, executionStatus, executionResult);
            String notificationType = "NODE_EXECUTION";

            // 获取目标用户
            List<String> targetUsers = getWorkflowNotificationTargets(workflowId, additionalData);

            // 只发送重要状态的通知
            if (isImportantExecutionStatus(executionStatus)) {
                sendSystemNotification(title, content, notificationType, targetUsers, additionalData);
            }

        } catch (Exception e) {
            log.error("发送节点执行通知失败", e);
        }
    }

    @Override
    @Async
    public void sendSyncCompletionNotification(String syncTaskId, String syncType,
                                               String status, String message,
                                               Map<String, Object> syncStats) {
        try {
            log.info("发送同步完成通知，任务ID: {}，类型: {}，状态: {}",
                    syncTaskId, syncType, status);

            // 构建通知内容
            String title = "数据同步完成";
            String content = String.format("同步任务 '%s' (%s) 已完成，状态: %s，详情: %s",
                    syncTaskId, syncType, status, message);
            String notificationType = "SYNC_COMPLETION";

            // 获取目标用户（同步任务相关人员）
            List<String> targetUsers = getSyncNotificationTargets(syncTaskId, syncType, syncStats);

            // 只发送失败或重要的同步通知
            if ("FAILED".equals(status) || isImportantSync(syncStats)) {
                sendSystemNotification(title, content, notificationType, targetUsers, syncStats);
            }

        } catch (Exception e) {
            log.error("发送同步完成通知失败", e);
        }
    }

    @Override
    @Async
    public void sendModeSwitchNotification(String oldMode, String newMode,
                                           String switchReason, String initiatedBy,
                                           Map<String, Object> additionalData) {
        try {
            log.info("发送模式切换通知，模式: {} -> {}，原因: {}",
                    oldMode, newMode, switchReason);

            // 构建通知内容
            String title = "系统模式切换";
            String content = String.format("系统模式已从 '%s' 切换为 '%s'，原因: %s，操作人: %s",
                    oldMode, newMode, switchReason, initiatedBy);
            String notificationType = "MODE_SWITCH";

            // 获取目标用户（所有在线用户）
            List<String> targetUsers = getAllOnlineUsers();

            // 发送通知
            sendSystemNotification(title, content, notificationType, targetUsers, additionalData);

        } catch (Exception e) {
            log.error("发送模式切换通知失败", e);
        }
    }

    @Override
    @Async
    public void sendBackupNotification(String backupId, String backupType,
                                       String status, String message,
                                       Map<String, Object> backupStats) {
        try {
            log.info("发送数据备份通知，备份ID: {}，类型: {}，状态: {}",
                    backupId, backupType, status);

            // 构建通知内容
            String title = "数据备份完成";
            String content = String.format("备份任务 '%s' (%s) 已完成，状态: %s，详情: %s",
                    backupId, backupType, status, message);
            String notificationType = "BACKUP_COMPLETION";

            // 获取目标用户（管理员）
            List<String> targetUsers = getAdminUsers();

            // 发送通知
            sendSystemNotification(title, content, notificationType, targetUsers, backupStats);

        } catch (Exception e) {
            log.error("发送数据备份通知失败", e);
        }
    }

    @Override
    @Async
    public void sendVersionCreationNotification(String workflowId, String workflowName,
                                                String versionNumber, String versionName,
                                                String createdBy, Map<String, Object> additionalData) {
        try {
            log.info("发送版本创建通知，工作流ID: {}，版本: {}",
                    workflowId, versionNumber);

            // 构建通知内容
            String title = "新版本创建";
            String content = String.format("工作流 '%s' 已创建新版本 '%s' (%s)，创建人: %s",
                    workflowName, versionName, versionNumber, createdBy);
            String notificationType = "VERSION_CREATION";

            // 获取目标用户（工作流相关人员）
            List<String> targetUsers = getWorkflowNotificationTargets(workflowId, additionalData);

            // 发送通知
            sendSystemNotification(title, content, notificationType, targetUsers, additionalData);

        } catch (Exception e) {
            log.error("发送版本创建通知失败", e);
        }
    }

    @Override
    @Async
    public void sendErrorNotification(String errorCode, String errorMessage,
                                      String module, String severity,
                                      Map<String, Object> errorData) {
        try {
            log.info("发送错误通知，错误码: {}，模块: {}，严重程度: {}",
                    errorCode, module, severity);

            // 构建通知内容
            String title = "系统错误";
            String content = String.format("模块 '%s' 发生错误，错误码: %s，消息: %s",
                    module, errorCode, errorMessage);
            String notificationType = "ERROR";

            // 获取目标用户（管理员）
            List<String> targetUsers = getAdminUsers();

            // 只发送重要错误通知
            if (isImportantError(severity)) {
                sendSystemNotification(title, content, notificationType, targetUsers, errorData);
            }

        } catch (Exception e) {
            log.error("发送错误通知失败", e);
        }
    }

    @Override
    @Async
    public void batchSendNotifications(List<Map<String, Object>> notifications) {
        try {
            log.info("批量发送通知，数量: {}", notifications.size());

            for (Map<String, Object> notification : notifications) {
                try {
                    String type = (String) notification.get("type");
                    String title = (String) notification.get("title");
                    String content = (String) notification.get("content");
                    @SuppressWarnings("unchecked")
                    List<String> targetUsers = (List<String>) notification.get("targetUsers");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> data = (Map<String, Object>) notification.get("data");

                    sendSystemNotification(title, content, type, targetUsers, data);

                } catch (Exception e) {
                    log.error("批量发送通知中的单个通知失败", e);
                }
            }

            log.info("批量通知发送完成");

        } catch (Exception e) {
            log.error("批量发送通知失败", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadNotificationCount(String userId) {
        try {
            return notificationRepository.countByUserIdAndReadFalse(userId);
        } catch (Exception e) {
            log.error("获取未读通知数量失败，用户ID: {}", userId, e);
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserNotifications(String userId, int limit, int offset) {
        try {
            List<NotificationEntity> notifications = notificationRepository
                    .findByUserIdOrderByCreateTimeDesc(userId, limit, offset);

            return notifications.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取用户通知列表失败，用户ID: {}", userId, e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional
    public void markNotificationAsRead(String notificationId, String userId) {
        try {
            NotificationEntity notification = notificationRepository
                    .findByIdAndUserId(notificationId, userId);

            if (notification != null) {
                notification.setRead(true);
                notification.setReadTime(LocalDateTime.now());
                notificationRepository.save(notification);

                log.debug("标记通知为已读，通知ID: {}，用户ID: {}", notificationId, userId);
            }

        } catch (Exception e) {
            log.error("标记通知为已读失败，通知ID: {}，用户ID: {}", notificationId, userId, e);
        }
    }

    @Override
    @Transactional
    public void deleteNotification(String notificationId, String userId) {
        try {
            NotificationEntity notification = notificationRepository
                    .findByIdAndUserId(notificationId, userId);

            if (notification != null) {
                notification.setDeleted(true);
                notification.setDeleteTime(LocalDateTime.now());
                notificationRepository.save(notification);

                log.debug("删除通知，通知ID: {}，用户ID: {}", notificationId, userId);
            }

        } catch (Exception e) {
            log.error("删除通知失败，通知ID: {}，用户ID: {}", notificationId, userId, e);
        }
    }

    @Override
    @Transactional
    public int cleanupExpiredNotifications(int daysToKeep) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);

            int deletedCount = notificationRepository.deleteByCreateTimeBefore(cutoffTime);

            log.info("清理过期通知完成，删除数量: {}，保留天数: {}", deletedCount, daysToKeep);
            return deletedCount;

        } catch (Exception e) {
            log.error("清理过期通知失败", e);
            return 0;
        }
    }

    // ==================== 私有方法 ====================

    private String buildConflictNotificationContent(Map<String, Object> conflictData) {
        StringBuilder content = new StringBuilder();

        content.append("发现新的数据冲突\n");
        content.append("====================\n\n");

        content.append("冲突ID: ").append(conflictData.get("conflictId")).append("\n");
        content.append("冲突类型: ").append(conflictData.get("conflictType")).append("\n");
        content.append("对象类型: ").append(conflictData.get("objectType")).append("\n");
        content.append("对象名称: ").append(conflictData.get("objectName")).append("\n");
        content.append("严重程度: ").append(conflictData.get("severity")).append("\n");
        content.append("检测时间: ").append(conflictData.get("detectedTime")).append("\n");

        if (conflictData.containsKey("description")) {
            content.append("描述: ").append(conflictData.get("description")).append("\n");
        }

        content.append("\n请及时登录系统处理此冲突。\n");

        return content.toString();
    }

    private String buildResolutionNotificationContent(Map<String, Object> resolutionData) {
        StringBuilder content = new StringBuilder();

        content.append("冲突已解决\n");
        content.append("====================\n\n");

        content.append("冲突ID: ").append(resolutionData.get("conflictId")).append("\n");
        content.append("对象类型: ").append(resolutionData.get("objectType")).append("\n");
        content.append("对象名称: ").append(resolutionData.get("objectName")).append("\n");
        content.append("解决策略: ").append(resolutionData.get("resolutionStrategy")).append("\n");
        content.append("解决人: ").append(resolutionData.get("resolvedBy")).append("\n");
        content.append("解决时间: ").append(resolutionData.get("resolvedTime")).append("\n");

        if (resolutionData.containsKey("resolutionNotes")) {
            content.append("解决备注: ").append(resolutionData.get("resolutionNotes")).append("\n");
        }

        return content.toString();
    }

    private List<String> getConflictNotificationTargets(Map<String, Object> conflictData) {
        List<String> targets = new ArrayList<>();

        // 添加管理员
        targets.addAll(getAdminUsers());

        // 添加冲突相关用户
        if (conflictData.containsKey("relatedUsers")) {
            @SuppressWarnings("unchecked")
            List<String> relatedUsers = (List<String>) conflictData.get("relatedUsers");
            targets.addAll(relatedUsers);
        }

        // 去重
        return targets.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getResolutionNotificationTargets(Map<String, Object> resolutionData) {
        List<String> targets = new ArrayList<>();

        // 添加冲突创建者（如果有）
        if (resolutionData.containsKey("createdBy")) {
            String createdBy = (String) resolutionData.get("createdBy");
            if (StringUtils.hasText(createdBy)) {
                targets.add(createdBy);
            }
        }

        // 添加相关用户
        if (resolutionData.containsKey("relatedUsers")) {
            @SuppressWarnings("unchecked")
            List<String> relatedUsers = (List<String>) resolutionData.get("relatedUsers");
            targets.addAll(relatedUsers);
        }

        // 去重
        return targets.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getWorkflowNotificationTargets(String workflowId, Map<String, Object> additionalData) {
        List<String> targets = new ArrayList<>();

        // 这里应该从数据库或缓存中获取工作流的相关用户
        // 暂时返回管理员列表
        targets.addAll(getAdminUsers());

        // 如果有额外数据中包含用户列表，也添加进来
        if (additionalData != null && additionalData.containsKey("notifyUsers")) {
            @SuppressWarnings("unchecked")
            List<String> notifyUsers = (List<String>) additionalData.get("notifyUsers");
            targets.addAll(notifyUsers);
        }

        return targets.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getSyncNotificationTargets(String syncTaskId, String syncType,
                                                    Map<String, Object> syncStats) {
        List<String> targets = new ArrayList<>();

        // 添加管理员
        targets.addAll(getAdminUsers());

        // 添加同步任务创建者
        if (syncStats != null && syncStats.containsKey("createdBy")) {
            String createdBy = (String) syncStats.get("createdBy");
            if (StringUtils.hasText(createdBy)) {
                targets.add(createdBy);
            }
        }

        return targets.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> getAllOnlineUsers() {
        // 这里应该获取所有在线用户的ID
        // 暂时返回空列表
        return Collections.emptyList();
    }

    private List<String> getAdminUsers() {
        // 这里应该从数据库或配置中获取管理员用户列表
        // 暂时返回默认管理员
        return Arrays.asList("admin", "system_admin");
    }

    private void sendWebNotification(String title, String content, String type,
                                     List<String> targetUsers, Map<String, Object> data) {
        try {
            // 这里应该实现WebSocket或Server-Sent Events推送
            log.debug("发送Web通知，标题: {}，目标用户: {}，类型: {}",
                    title, targetUsers.size(), type);

            // 实际实现应该调用WebSocket服务推送消息

        } catch (Exception e) {
            log.error("发送Web通知失败", e);
        }
    }

    private void sendEmailNotification(String title, String content,
                                       List<String> targetUsers, Map<String, Object> data) {
        if (mailSender == null) {
            log.warn("邮件发送器未配置，跳过邮件通知");
            return;
        }

        try {
            for (String user : targetUsers) {
                // 获取用户邮箱（这里应该从用户服务获取）
                String email = getUserEmail(user);
                if (!StringUtils.hasText(email)) {
                    continue;
                }

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(emailFrom);
                helper.setTo(email);
                helper.setSubject("[工作流平台] " + title);

                // 构建HTML内容
                String htmlContent = buildEmailHtmlContent(title, content, data);
                helper.setText(htmlContent, true);

                mailSender.send(message);

                log.debug("邮件通知发送成功，收件人: {}，标题: {}", email, title);
            }

        } catch (Exception e) {
            log.error("发送邮件通知失败", e);
        }
    }

    private void sendPushNotification(String title, String content,
                                      List<String> targetUsers, Map<String, Object> data) {
        try {
            // 这里应该实现推送通知（如FCM、APNs等）
            log.debug("发送推送通知，标题: {}，目标用户: {}，类型: {}",
                    title, targetUsers.size(), data.get("type"));

        } catch (Exception e) {
            log.error("发送推送通知失败", e);
        }
    }

    private void recordNotification(String title, String content, String type,
                                    List<String> targetUsers, Map<String, Object> data) {
        try {
            for (String userId : targetUsers) {
                NotificationEntity notification = new NotificationEntity();
                notification.setUserId(userId);
                notification.setTitle(title);
                notification.setContent(content);
                notification.setType(type);
                notification.setRead(false);
                notification.setCreateTime(LocalDateTime.now());

                if (data != null && !data.isEmpty()) {
                    notification.setData(JsonUtil.toJson(data));
                }

                notificationRepository.save(notification);
            }

        } catch (Exception e) {
            log.error("记录通知失败", e);
        }
    }

    private String getUserEmail(String userId) {
        // 这里应该从用户服务获取用户邮箱
        // 暂时返回默认邮箱格式
        return userId + "@example.com";
    }

    private String buildEmailHtmlContent(String title, String content, Map<String, Object> data) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"zh-CN\">");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>").append(title).append("</title>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background-color: #1890ff; color: white; padding: 20px; text-align: center; }");
        html.append(".content { padding: 20px; background-color: #f9f9f9; }");
        html.append(".footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }");
        html.append(".button { display: inline-block; padding: 10px 20px; background-color: #1890ff; color: white; text-decoration: none; border-radius: 4px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class=\"container\">");
        html.append("<div class=\"header\">");
        html.append("<h1>").append(title).append("</h1>");
        html.append("</div>");
        html.append("<div class=\"content\">");
        html.append("<p>").append(content.replace("\n", "<br>")).append("</p>");

        if (data != null && !data.isEmpty()) {
            html.append("<h3>详细信息：</h3>");
            html.append("<ul>");
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (!"type".equals(entry.getKey())) {
                    html.append("<li><strong>").append(entry.getKey()).append(":</strong> ")
                            .append(entry.getValue()).append("</li>");
                }
            }
            html.append("</ul>");
        }

        html.append("<p><a href=\"").append(getSystemUrl()).append("\" class=\"button\">")
                .append("登录系统查看</a></p>");
        html.append("</div>");
        html.append("<div class=\"footer\">");
        html.append("<p>这是一封自动发送的通知邮件，请勿直接回复。</p>");
        html.append("<p>© 2024 工作流平台. 版权所有.</p>");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private String getSystemUrl() {
        // 这里应该返回系统URL
        return "http://localhost:8080";
    }

    private boolean shouldSendEmail(String notificationType) {
        // 根据通知类型决定是否发送邮件
        return "ERROR".equals(notificationType) ||
                "CONFLICT".equals(notificationType) ||
                "MODE_SWITCH".equals(notificationType);
    }

    private boolean shouldSendPush(String notificationType) {
        // 根据通知类型决定是否发送推送
        return "ERROR".equals(notificationType) ||
                "CONFLICT".equals(notificationType);
    }

    private boolean isImportantExecutionStatus(String status) {
        return "FAILED".equals(status) ||
                "TIMEOUT".equals(status) ||
                "ERROR".equals(status);
    }

    private boolean isImportantSync(Map<String, Object> syncStats) {
        if (syncStats == null) return false;

        Object conflictCount = syncStats.get("conflictCount");
        if (conflictCount instanceof Number) {
            return ((Number) conflictCount).intValue() > 0;
        }

        return false;
    }

    private boolean isImportantError(String severity) {
        return "HIGH".equals(severity) || "CRITICAL".equals(severity);
    }

    private Map<String, Object> convertToMap(NotificationEntity notification) {
        Map<String, Object> map = new HashMap<>();

        map.put("id", notification.getId());
        map.put("title", notification.getTitle());
        map.put("content", notification.getContent());
        map.put("type", notification.getType());
        map.put("read", notification.isRead());
        map.put("createTime", notification.getCreateTime());
        map.put("readTime", notification.getReadTime());

        if (StringUtils.hasText(notification.getData())) {
            try {
                Map<String, Object> data = JsonUtil.fromJson(notification.getData(), Map.class);
                map.put("data", data);
            } catch (Exception e) {
                log.warn("解析通知数据失败: {}", notification.getData());
            }
        }

        return map;
    }
}