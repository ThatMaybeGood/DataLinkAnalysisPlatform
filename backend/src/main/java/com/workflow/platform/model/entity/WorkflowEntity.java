package com.workflow.platform.model.entity;

import entity.ExecutionEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

/**
 * 工作流实体类
 * 对应数据库中的workflows表（在线模式）
 * 离线模式下对应JSON文件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "workflows",
        indexes = {
                @Index(name = "idx_name", columnList = "name"),
                @Index(name = "idx_alias", columnList = "alias"),
                @Index(name = "idx_category", columnList = "category"),
                @Index(name = "idx_status", columnList = "status"),
                @Index(name = "idx_created_at", columnList = "createdAt")
        })
public class WorkflowEntity extends com.workflow.platform.model.entity.BaseEntity {

    /**
     * 主键ID，使用UUID生成策略
     */
    @Id
    @Column(name = "id", length = 50)
    private String id;

    /**
     * 工作流名称
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * 工作流别名（用于快速搜索）
     */
    @Column(name = "alias", unique = true, length = 100)
    private String alias;

    /**
     * 工作流描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 分类
     */
    @Column(name = "category", length = 100)
    private String category;

    /**
     * 标签（JSON格式存储）
     */
    @Column(name = "tags", columnDefinition = "JSON")
    private String tags; // 存储为JSON字符串

    /**
     * 工作流配置（JSON格式）
     */
    @Column(name = "config", columnDefinition = "JSON")
    private String config;

    /**
     * 状态
     * draft: 草稿
     * active: 活跃
     * inactive: 未激活
     * archived: 已归档
     */
    @Column(name = "status", length = 20)
    private String status = "draft";

    /**
     * 版本号
     */
    @Column(name = "version", length = 20)
    private String version = "1.0";

    /**
     * 模式：online/offline
     * 用于区分数据来源
     */
    @Column(name = "mode", length = 20)
    private String mode = "online";

    /**
     * 节点数量（统计字段）
     */
    @Column(name = "node_count")
    private Integer nodeCount = 0;

    /**
     * 执行次数（统计字段）
     */
    @Column(name = "execution_count")
    private Integer executionCount = 0;

    /**
     * 成功率百分比（0-100）
     */
    @Column(name = "success_rate", precision = 5, scale = 2)
    private Double successRate = 0.0;

    /**
     * 平均执行耗时（毫秒）
     */
    @Column(name = "avg_duration")
    private Integer avgDuration = 0;

    /**
     * 租户ID（多租户支持）
     */
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    /**
     * 关联的节点（一对多关系）
     */
    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<NodeEntity> nodes;

    /**
     * 关联的执行记录（一对多关系）
     */
    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExecutionEntity> executions;

    /**
     * 默认构造函数
     */
    public WorkflowEntity() {
        // JPA需要无参构造函数
    }

    /**
     * 带参数的构造函数
     */
    public WorkflowEntity(String id, String name, String alias) {
        this.id = id;
        this.name = name;
        this.alias = alias;
    }
}