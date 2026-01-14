package com.workflow.platform.model.entity;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/10 23:50
 */


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.poi.ss.formula.functions.T;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流版本实体
 */
@Entity
@Table(name = "workflow_version")
@Data
@EqualsAndHashCode(callSuper = false)
public class WorkflowVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 工作流ID
     */
    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    /**
     * 版本号
     */
    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    /**
     * 版本名称
     */
    @Column(name = "version_name")
    private String versionName;

    /**
     * 版本描述
     */
    @Column(name = "description")
    private String description;

    /**
     * 工作流数据（JSON格式）
     */
    @Lob
    @Column(name = "workflow_data", nullable = false)
    private String workflowData;

    /**
     * 节点数据（JSON格式）
     */
    @Lob
    @Column(name = "node_data")
    private String nodeData;

    /**
     * 验证规则数据（JSON格式）
     */
    @Lob
    @Column(name = "validation_data")
    private String validationData;

    /**
     * 创建人
     */
    @Column(name = "created_by")
    private String createdBy;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /**
     * 是否为当前版本
     */
    @Column(name = "is_current")
    private Boolean isCurrent;

    /**
     * 版本标签
     */
    @Column(name = "tags")
    private String tags;

    /**
     * 变更摘要
     */
    @Column(name = "change_summary")
    private String changeSummary;

    /**
     * 元数据（JSON格式）
     */
    @Lob
    @Column(name = "metadata")
    private Map<String,Object> metadata;

    private LocalDateTime createdTime;

    private String status;

    private String versionData;
    private String checksum;
    private Integer dataSize;
    private Integer rollbackFromVersion;
    private String rollbackReason;
    private String branchId;
    private String branchName;
    private Integer basedOnVersion;
    private String mergeFromBranch;
    private String mergeStrategy;
    private Integer mergeBaseVersion;
    private LocalDateTime deletedTime;
    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (isCurrent == null) {
            isCurrent = false;
        }
    }
}