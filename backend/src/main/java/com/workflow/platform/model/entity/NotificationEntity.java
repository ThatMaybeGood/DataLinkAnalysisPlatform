package com.workflow.platform.model.entity;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:53
 */

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 通知实体
 */
@Entity
@Table(name = "notification")
@Data
@EqualsAndHashCode(callSuper = false)
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * 通知标题
     */
    @Column(name = "title", nullable = false)
    private String title;

    /**
     * 通知内容
     */
    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    /**
     * 通知类型
     */
    @Column(name = "type", nullable = false)
    private String type;

    /**
     * 附加数据（JSON格式）
     */
    @Lob
    @Column(name = "data")
    private String data;

    /**
     * 是否已读
     */
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    /**
     * 阅读时间
     */
    @Column(name = "read_time")
    private LocalDateTime readTime;

    /**
     * 是否已删除
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    /**
     * 删除时间
     */
    @Column(name = "delete_time")
    private LocalDateTime deleteTime;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 过期时间
     */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    /**
     * 优先级：LOW, MEDIUM, HIGH, URGENT
     */
    @Column(name = "priority")
    private String priority = "MEDIUM";

    /**
     * 发送渠道：WEB, EMAIL, SMS, PUSH
     */
    @Column(name = "channel")
    private String channel = "WEB";

    /**
     * 发送状态：PENDING, SENT, FAILED
     */
    @Column(name = "send_status")
    private String sendStatus = "SENT";

    /**
     * 相关对象ID
     */
    @Column(name = "related_object_id")
    private String relatedObjectId;

    /**
     * 相关对象类型
     */
    @Column(name = "related_object_type")
    private String relatedObjectType;

    /**
     * 操作链接
     */
    @Column(name = "action_url")
    private String actionUrl;

    /**
     * 操作文本
     */
    @Column(name = "action_text")
    private String actionText;
}