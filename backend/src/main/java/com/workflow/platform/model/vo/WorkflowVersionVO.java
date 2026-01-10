package com.workflow.platform.model.vo;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:05
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流版本视图对象
 */
@Data
@ApiModel(description = "工作流版本视图对象")
public class WorkflowVersionVO {

    @ApiModelProperty(value = "版本ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "工作流ID", example = "1")
    private Long workflowId;

    @ApiModelProperty(value = "工作流名称", example = "订单处理流程")
    private String workflowName;

    @ApiModelProperty(value = "版本号", example = "1")
    private Integer versionNumber;

    @ApiModelProperty(value = "版本名称", example = "v1.0.0")
    private String versionName;

    @ApiModelProperty(value = "版本描述", example = "初始版本")
    private String description;

    @ApiModelProperty(value = "创建人", example = "admin")
    private String createdBy;

    @ApiModelProperty(value = "创建人姓名", example = "系统管理员")
    private String createdByName;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "是否为当前版本", example = "true")
    private Boolean isCurrent;

    @ApiModelProperty(value = "版本标签", example = "stable,draft,release")
    private String tags;

    @ApiModelProperty(value = "标签列表")
    private List<String> tagList;

    @ApiModelProperty(value = "变更摘要", example = "新增了验证节点")
    private String changeSummary;

    @ApiModelProperty(value = "工作流数据大小（字节）", example = "2048")
    private Integer dataSize;

    @ApiModelProperty(value = "工作流节点数量", example = "10")
    private Integer nodeCount;

    @ApiModelProperty(value = "验证规则数量", example = "5")
    private Integer validationCount;

    @ApiModelProperty(value = "元数据")
    private String metadata;

    @ApiModelProperty(value = "版本状态", example = "ACTIVE")
    private String status;

    @ApiModelProperty(value = "是否可以回滚", example = "true")
    private Boolean canRollback;

    @ApiModelProperty(value = "是否可以删除", example = "true")
    private Boolean canDelete;

    @ApiModelProperty(value = "版本统计信息")
    private VersionStatistics statistics;

    @ApiModelProperty(value = "关联文件信息")
    private List<VersionFileInfo> fileInfos;

    /**
     * 版本统计信息
     */
    @Data
    @ApiModel(description = "版本统计信息")
    public static class VersionStatistics {

        @ApiModelProperty(value = "节点总数", example = "10")
        private Integer totalNodes;

        @ApiModelProperty(value = "起始节点数", example = "1")
        private Integer startNodes;

        @ApiModelProperty(value = "结束节点数", example = "1")
        private Integer endNodes;

        @ApiModelProperty(value = "动作节点数", example = "5")
        private Integer actionNodes;

        @ApiModelProperty(value = "决策节点数", example = "2")
        private Integer decisionNodes;

        @ApiModelProperty(value = "连接线数量", example = "12")
        private Integer connections;

        @ApiModelProperty(value = "验证规则数量", example = "3")
        private Integer validationRules;

        @ApiModelProperty(value = "输入参数数量", example = "5")
        private Integer inputParams;

        @ApiModelProperty(value = "输出参数数量", example = "3")
        private Integer outputParams;

        @ApiModelProperty(value = "平均节点复杂度", example = "2.5")
        private Double avgNodeComplexity;
    }

    /**
     * 版本文件信息
     */
    @Data
    @ApiModel(description = "版本文件信息")
    public static class VersionFileInfo {

        @ApiModelProperty(value = "文件ID", example = "file_001")
        private String fileId;

        @ApiModelProperty(value = "文件名称", example = "工作流定义.json")
        private String fileName;

        @ApiModelProperty(value = "文件类型", example = "json")
        private String fileType;

        @ApiModelProperty(value = "文件大小", example = "2048")
        private Long fileSize;

        @ApiModelProperty(value = "文件路径", example = "/workflows/1/version_1.json")
        private String filePath;

        @ApiModelProperty(value = "MD5哈希值", example = "d41d8cd98f00b204e9800998ecf8427e")
        private String md5Hash;

        @ApiModelProperty(value = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;

        @ApiModelProperty(value = "是否压缩", example = "true")
        private Boolean compressed;

        @ApiModelProperty(value = "是否加密", example = "false")
        private Boolean encrypted;
    }

    /**
     * 版本差异摘要
     */
    @Data
    @ApiModel(description = "版本差异摘要")
    public static class VersionDiffSummary {

        @ApiModelProperty(value = "差异总数", example = "5")
        private Integer totalDiffs;

        @ApiModelProperty(value = "新增数量", example = "2")
        private Integer addedCount;

        @ApiModelProperty(value = "修改数量", example = "2")
        private Integer modifiedCount;

        @ApiModelProperty(value = "删除数量", example = "1")
        private Integer deletedCount;

        @ApiModelProperty(value = "移动数量", example = "0")
        private Integer movedCount;

        @ApiModelProperty(value = "受影响节点列表")
        private List<String> affectedNodes;

        @ApiModelProperty(value = "受影响验证规则列表")
        private List<String> affectedValidations;

        @ApiModelProperty(value = "主要变更类型", example = "节点新增")
        private String mainChangeType;
    }

    /**
     * 版本预览信息
     */
    @Data
    @ApiModel(description = "版本预览信息")
    public static class VersionPreview {

        @ApiModelProperty(value = "工作流基本信息")
        private WorkflowBasicInfo workflowInfo;

        @ApiModelProperty(value = "节点预览列表")
        private List<NodePreview> nodePreviews;

        @ApiModelProperty(value = "验证规则预览列表")
        private List<ValidationPreview> validationPreviews;

        @ApiModelProperty(value = "连接线预览列表")
        private List<ConnectionPreview> connectionPreviews;
    }

    /**
     * 工作流基本信息
     */
    @Data
    @ApiModel(description = "工作流基本信息")
    public static class WorkflowBasicInfo {

        @ApiModelProperty(value = "工作流名称", example = "订单处理流程")
        private String name;

        @ApiModelProperty(value = "工作流描述", example = "处理客户订单的完整流程")
        private String description;

        @ApiModelProperty(value = "工作流类型", example = "ORDER_PROCESSING")
        private String type;

        @ApiModelProperty(value = "工作流状态", example = "ACTIVE")
        private String status;

        @ApiModelProperty(value = "版本号", example = "v1.0.0")
        private String version;

        @ApiModelProperty(value = "分类ID", example = "1")
        private Long categoryId;

        @ApiModelProperty(value = "分类名称", example = "业务流程")
        private String categoryName;

        @ApiModelProperty(value = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;

        @ApiModelProperty(value = "更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updateTime;
    }

    /**
     * 节点预览信息
     */
    @Data
    @ApiModel(description = "节点预览信息")
    public static class NodePreview {

        @ApiModelProperty(value = "节点ID", example = "node_001")
        private String nodeId;

        @ApiModelProperty(value = "节点名称", example = "开始节点")
        private String nodeName;

        @ApiModelProperty(value = "节点类型", example = "START_NODE")
        private String nodeType;

        @ApiModelProperty(value = "节点描述", example = "流程开始节点")
        private String description;

        @ApiModelProperty(value = "位置X坐标", example = "100")
        private Integer positionX;

        @ApiModelProperty(value = "位置Y坐标", example = "100")
        private Integer positionY;

        @ApiModelProperty(value = "是否启用", example = "true")
        private Boolean enabled;

        @ApiModelProperty(value = "配置信息")
        private String config;
    }

    /**
     * 验证规则预览信息
     */
    @Data
    @ApiModel(description = "验证规则预览信息")
    public static class ValidationPreview {

        @ApiModelProperty(value = "规则ID", example = "rule_001")
        private String ruleId;

        @ApiModelProperty(value = "规则名称", example = "必填验证")
        private String ruleName;

        @ApiModelProperty(value = "规则类型", example = "REQUIRED")
        private String ruleType;

        @ApiModelProperty(value = "规则描述", example = "验证字段是否必填")
        private String description;

        @ApiModelProperty(value = "目标字段", example = "userName")
        private String targetField;

        @ApiModelProperty(value = "错误信息", example = "用户名不能为空")
        private String errorMessage;

        @ApiModelProperty(value = "是否启用", example = "true")
        private Boolean enabled;
    }

    /**
     * 连接线预览信息
     */
    @Data
    @ApiModel(description = "连接线预览信息")
    public static class ConnectionPreview {

        @ApiModelProperty(value = "连接线ID", example = "conn_001")
        private String connectionId;

        @ApiModelProperty(value = "源节点ID", example = "node_001")
        private String sourceNodeId;

        @ApiModelProperty(value = "目标节点ID", example = "node_002")
        private String targetNodeId;

        @ApiModelProperty(value = "连接线标签", example = "成功")
        private String label;

        @ApiModelProperty(value = "连接线类型", example = "SUCCESS")
        private String connectionType;

        @ApiModelProperty(value = "条件表达式")
        private String condition;

        @ApiModelProperty(value = "是否启用", example = "true")
        private Boolean enabled;
    }

    /**
     * 版本变更记录
     */
    @Data
    @ApiModel(description = "版本变更记录")
    public static class VersionChangeRecord {

        @ApiModelProperty(value = "变更ID", example = "change_001")
        private String changeId;

        @ApiModelProperty(value = "变更类型", example = "NODE_ADDED")
        private String changeType;

        @ApiModelProperty(value = "变更描述", example = "新增了用户验证节点")
        private String changeDescription;

        @ApiModelProperty(value = "变更对象ID", example = "node_003")
        private String objectId;

        @ApiModelProperty(value = "变更对象类型", example = "NODE")
        private String objectType;

        @ApiModelProperty(value = "变更前值")
        private String oldValue;

        @ApiModelProperty(value = "变更后值")
        private String newValue;

        @ApiModelProperty(value = "变更时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime changeTime;

        @ApiModelProperty(value = "变更人", example = "admin")
        private String changedBy;
    }

    /**
     * 版本导出信息
     */
    @Data
    @ApiModel(description = "版本导出信息")
    public static class VersionExportInfo {

        @ApiModelProperty(value = "导出ID", example = "export_001")
        private String exportId;

        @ApiModelProperty(value = "导出格式", example = "json")
        private String exportFormat;

        @ApiModelProperty(value = "导出时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime exportTime;

        @ApiModelProperty(value = "导出人", example = "admin")
        private String exportedBy;

        @ApiModelProperty(value = "文件大小", example = "4096")
        private Long fileSize;

        @ApiModelProperty(value = "文件路径", example = "/exports/version_1_export.json")
        private String filePath;

        @ApiModelProperty(value = "是否包含元数据", example = "true")
        private Boolean includeMetadata;

        @ApiModelProperty(value = "是否压缩", example = "true")
        private Boolean compressed;
    }

    /**
     * 版本依赖关系
     */
    @Data
    @ApiModel(description = "版本依赖关系")
    public static class VersionDependency {

        @ApiModelProperty(value = "依赖ID", example = "dep_001")
        private String dependencyId;

        @ApiModelProperty(value = "依赖类型", example = "PARENT_VERSION")
        private String dependencyType;

        @ApiModelProperty(value = "依赖版本ID", example = "1")
        private Long dependentVersionId;

        @ApiModelProperty(value = "依赖版本号", example = "v1.0.0")
        private String dependentVersionNumber;

        @ApiModelProperty(value = "依赖描述", example = "基于v1.0.0版本修改")
        private String description;
    }
}