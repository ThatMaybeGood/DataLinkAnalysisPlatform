package com.workflow.platform.model.entity;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:21
 */

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 冲突记录实体
 */
@Entity
@Table(name = "conflict_record")
@Data
@EqualsAndHashCode(callSuper = false)
public class ConflictRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 冲突类型
     */
    @Column(name = "conflict_type", nullable = false)
    private String conflictType;

    /**
     * 对象类型：WORKFLOW, NODE, VALIDATION, CATEGORY
     */
    @Column(name = "object_type", nullable = false)
    private String objectType;

    /**
     * 对象ID
     */
    @Column(name = "object_id")
    private String objectId;

    /**
     * 对象名称
     */
    @Column(name = "object_name")
    private String objectName;

    /**
     * 本地数据（JSON格式）
     */
    @Lob
    @Column(name = "local_data")
    private String localData;

    /**
     * 远程数据（JSON格式）
     */
    @Lob
    @Column(name = "remote_data")
    private String remoteData;

    /**
     * 本地数据版本号
     */
    @Column(name = "local_version")
    private Integer localVersion;

    /**
     * 远程数据版本号
     */
    @Column(name = "remote_version")
    private Integer remoteVersion;

    /**
     * 本地更新时间
     */
    @Column(name = "local_update_time")
    private LocalDateTime localUpdateTime;

    /**
     * 远程更新时间
     */
    @Column(name = "remote_update_time")
    private LocalDateTime remoteUpdateTime;

    /**
     * 冲突解决状态：PENDING, RESOLVED, IGNORED, AUTO_RESOLVED
     */
    @Column(name = "status", nullable = false)
    private String status = "PENDING";

    /**
     * 解决策略：CLIENT_PRIORITY, SERVER_PRIORITY, TIMESTAMP_PRIORITY, MANUAL, MERGE
     */
    @Column(name = "resolution_strategy")
    private String resolutionStrategy;

    /**
     * 解决结果（JSON格式）
     */
    @Lob
    @Column(name = "resolution_result")
    private String resolutionResult;

    /**
     * 解决人
     */
    @Column(name = "resolved_by")
    private String resolvedBy;

    /**
     * 解决时间
     */
    @Column(name = "resolved_time")
    private LocalDateTime resolvedTime;

    /**
     * 解决备注
     */
    @Column(name = "resolution_notes")
    private String resolutionNotes;

    /**
     * 同步任务ID
     */
    @Column(name = "sync_task_id")
    private String syncTaskId;

    /**
     * 设备/客户端标识
     */
    @Column(name = "client_id")
    private String clientId;

    /**
     * IP地址
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * 冲突检测时间
     */
    @CreationTimestamp
    @Column(name = "detected_time", nullable = false, updatable = false)
    private LocalDateTime detectedTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 元数据（JSON格式）
     */
    @Lob
    @Column(name = "metadata")
    private String metadata;

    /**
     * 冲突详细描述
     */
    @Column(name = "description")
    private String description;

    /**
     * 冲突严重程度：LOW, MEDIUM, HIGH, CRITICAL
     */
    @Column(name = "severity")
    private String severity = "MEDIUM";

    /**
     * 是否已通知
     */
    @Column(name = "notified")
    private Boolean notified = false;

    /**
     * 重试次数
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /**
     * 最后重试时间
     */
    @Column(name = "last_retry_time")
    private LocalDateTime lastRetryTime;

    /**
     * 冲突哈希值（用于去重）
     */
    @Column(name = "conflict_hash")
    private String conflictHash;

    /**
     * 是否自动解决
     */
    @Column(name = "auto_resolved")
    private Boolean autoResolved = false;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;
}