package com.workflow.platform.model.vo;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:08
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 工作流版本详情视图对象
 *
 * 4. WorkflowVersionDetailVO
 *
 * 用于版本详情展示
 * 包含完整的工作流配置、节点、验证规则等信息
 * 支持详细的版本分析
 *
 *
 */
@Data
@ApiModel(description = "工作流版本详情视图对象")
public class WorkflowVersionDetailVO {

    @ApiModelProperty(value = "版本基本信息")
    private VersionBasicInfo basicInfo;

    @ApiModelProperty(value = "工作流配置信息")
    private WorkflowConfigInfo workflowConfig;

    @ApiModelProperty(value = "节点信息列表")
    private List<NodeInfo> nodes;

    @ApiModelProperty(value = "验证规则列表")
    private List<ValidationRuleInfo> validationRules;

    @ApiModelProperty(value = "连接线信息列表")
    private List<ConnectionInfo> connections;

    @ApiModelProperty(value = "输入参数列表")
    private List<ParameterInfo> inputParameters;

    @ApiModelProperty(value = "输出参数列表")
    private List<ParameterInfo> outputParameters;

    @ApiModelProperty(value = "元数据信息")
    private Map<String, Object> metadata;

    @ApiModelProperty(value = "统计信息")
    private VersionStatistics statistics;

    @ApiModelProperty(value = "变更记录")
    private List<ChangeRecord> changeRecords;

    @ApiModelProperty(value = "依赖关系")
    private List<DependencyInfo> dependencies;

    @ApiModelProperty(value = "相关文件")
    private List<RelatedFile> relatedFiles;

    /**
     * 版本基本信息
     */
    @Data
    @ApiModel(description = "版本基本信息")
    public static class VersionBasicInfo {

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

        @ApiModelProperty(value = "标签列表")
        private List<String> tags;

        @ApiModelProperty(value = "变更摘要", example = "新增了验证节点")
        private String changeSummary;

        @ApiModelProperty(value = "数据大小", example = "2048")
        private Integer dataSize;

        @ApiModelProperty(value = "版本状态", example = "ACTIVE")
        private String status;

        @ApiModelProperty(value = "版本类型", example = "MAJOR")
        private String versionType;

        @ApiModelProperty(value = "父版本ID", example = "1")
        private Long parentVersionId;

        @ApiModelProperty(value = "父版本号", example = "v1.0.0")
        private String parentVersionNumber;
    }

    /**
     * 工作流配置信息
     */
    @Data
    @ApiModel(description = "工作流配置信息")
    public static class WorkflowConfigInfo {

        @ApiModelProperty(value = "工作流类型", example = "ORDER_PROCESSING")
        private String type;

        @ApiModelProperty(value = "工作流状态", example = "ACTIVE")
        private String status;

        @ApiModelProperty(value = "是否启用", example = "true")
        private Boolean enabled;

        @ApiModelProperty(value = "超时时间（秒）", example = "3600")
        private Integer timeout;

        @ApiModelProperty(value = "重试次数", example = "3")
        private Integer retryCount;

        @ApiModelProperty(value = "并发限制", example = "10")
        private Integer concurrencyLimit;

        @ApiModelProperty(value = "优先级", example = "5")
        private Integer priority;

        @ApiModelProperty(value = "执行引擎", example = "STANDARD")
        private String executionEngine;

        @ApiModelProperty(value = "通知配置")
        private Map<String, Object> notificationConfig;

        @ApiModelProperty(value = "日志配置")
        private Map<String, Object> loggingConfig;

        @ApiModelProperty(value = "监控配置")
        private Map<String, Object> monitoringConfig;
    }

    /**
     * 节点信息
     */
    @Data
    @ApiModel(description = "节点信息")
    public static class NodeInfo {

        @ApiModelProperty(value = "节点ID", example = "node_001")
        private String nodeId;

        @ApiModelProperty(value = "节点名称", example = "开始节点")
        private String name;

        @ApiModelProperty(value = "节点类型", example = "START_NODE")
        private String type;

        @ApiModelProperty(value = "节点描述", example = "流程开始节点")
        private String description;

        @ApiModelProperty(value = "位置X坐标", example = "100")
        private Integer positionX;

        @ApiModelProperty(value = "位置Y坐标", example = "100")
        private Integer positionY;

        @ApiModelProperty(value = "宽度", example = "120")
        private Integer width;

        @ApiModelProperty(value = "高度", example = "80")
        private Integer height;

        @ApiModelProperty(value = "是否启用", example = "true")
        private Boolean enabled;

        @ApiModelProperty(value = "执行条件")
        private String executionCondition;

        @ApiModelProperty(value = "超时时间", example = "300")
        private Integer timeout;

        @ApiModelProperty(value = "重试配置")
        private Map<String, Object> retryConfig;

        @ApiModelProperty(value = "输入映射")
        private Map<String, Object> inputMapping;

        @ApiModelProperty(value = "输出映射")
        private Map<String, Object> outputMapping;

        @ApiModelProperty(value = "配置信息")
        private Map<String, Object> config;

        @ApiModelProperty(value = "自定义属性")
        private Map<String, Object> customProperties;

        @ApiModelProperty(value = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;

        @ApiModelProperty(value = "更新时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime updateTime;
    }

    /**
     * 验证规则信息
     */
    @Data
    @ApiModel(description = "验证规则信息")
    public static class ValidationRuleInfo {

        @ApiModelProperty(value = "规则ID", example = "rule_001")
        private String ruleId;

        @ApiModelProperty(value = "规则名称", example = "必填验证")
        private String name;

        @ApiModelProperty(value = "规则类型", example = "REQUIRED")
        private String type;

        @ApiModelProperty(value = "规则描述", example = "验证字段是否必填")
        private String description;

        @ApiModelProperty(value = "目标字段", example = "userName")
        private String targetField;

        @ApiModelProperty(value = "验证条件")
        private String condition;

        @ApiModelProperty(value = "错误信息", example = "用户名不能为空")
        private String errorMessage;

        @ApiModelProperty(value = "错误级别", example = "ERROR")
        private String errorLevel;

        @ApiModelProperty(value = "是否启用", example = "true")
        private Boolean enabled;

        @ApiModelProperty(value = "执行顺序", example = "1")
        private Integer executionOrder;

        @ApiModelProperty(value = "适用节点列表")
        private List<String> applicableNodes;

        @ApiModelProperty(value = "配置信息")
        private Map<String, Object> config;

        @ApiModelProperty(value = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;
    }

    /**
     * 连接线信息
     */
    @Data
    @ApiModel(description = "连接线信息")
    public static class ConnectionInfo {

        @ApiModelProperty(value = "连接线ID", example = "conn_001")
        private String connectionId;

        @ApiModelProperty(value = "源节点ID", example = "node_001")
        private String sourceNodeId;

        @ApiModelProperty(value = "目标节点ID", example = "node_002")
        private String targetNodeId;

        @ApiModelProperty(value = "连接线类型", example = "SUCCESS")
        private String type;

        @ApiModelProperty(value = "标签", example = "成功")
        private String label;

        @ApiModelProperty(value = "条件表达式")
        private String condition;

        @ApiModelProperty(value = "是否启用", example = "true")
        private Boolean enabled;

        @ApiModelProperty(value = "优先级", example = "1")
        private Integer priority;

        @ApiModelProperty(value = "样式配置")
        private Map<String, Object> styleConfig;

        @ApiModelProperty(value = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;
    }

    /**
     * 参数信息
     */
    @Data
    @ApiModel(description = "参数信息")
    public static class ParameterInfo {

        @ApiModelProperty(value = "参数ID", example = "param_001")
        private String paramId;

        @ApiModelProperty(value = "参数名称", example = "userName")
        private String name;

        @ApiModelProperty(value = "参数类型", example = "STRING")
        private String type;

        @ApiModelProperty(value = "参数描述", example = "用户名")
        private String description;

        @ApiModelProperty(value = "是否必填", example = "true")
        private Boolean required;

        @ApiModelProperty(value = "默认值")
        private String defaultValue;

        @ApiModelProperty(value = "验证规则")
        private List<String> validationRules;

        @ApiModelProperty(value = "示例值", example = "admin")
        private String exampleValue;
    }

    /**
     * 版本统计信息
     */
    @Data
    @ApiModel(description = "版本统计信息")
    public static class VersionStatistics {

        @ApiModelProperty(value = "节点总数", example = "10")
        private Integer totalNodes;

        @ApiModelProperty(value = "节点类型分布")
        private Map<String, Integer> nodeTypeDistribution;

        @ApiModelProperty(value = "连接线总数", example = "12")
        private Integer totalConnections;

        @ApiModelProperty(value = "验证规则总数", example = "5")
        private Integer totalValidationRules;

        @ApiModelProperty(value = "验证规则类型分布")
        private Map<String, Integer> validationTypeDistribution;

        @ApiModelProperty(value = "输入参数总数", example = "5")
        private Integer totalInputParameters;

        @ApiModelProperty(value = "输出参数总数", example = "3")
        private Integer totalOutputParameters;

        @ApiModelProperty(value = "平均节点复杂度", example = "2.5")
        private Double avgNodeComplexity;

        @ApiModelProperty(value = "最大节点深度", example = "3")
        private Integer maxNodeDepth;

        @ApiModelProperty(value = "最长路径长度", example = "5")
        private Integer longestPathLength;
    }

    /**
     * 变更记录
     */
    @Data
    @ApiModel(description = "变更记录")
    public static class ChangeRecord {

        @ApiModelProperty(value = "变更ID", example = "change_001")
        private String changeId;

        @ApiModelProperty(value = "变更类型", example = "NODE_ADDED")
        private String changeType;

        @ApiModelProperty(value = "变更描述", example = "新增了用户验证节点")
        private String description;

        @ApiModelProperty(value = "变更对象ID", example = "node_003")
        private String objectId;

        @ApiModelProperty(value = "变更对象类型", example = "NODE")
        private String objectType;

        @ApiModelProperty(value = "变更前值")
        private Map<String, Object> oldValue;

        @ApiModelProperty(value = "变更后值")
        private Map<String, Object> newValue;

        @ApiModelProperty(value = "变更人", example = "admin")
        private String changedBy;

        @ApiModelProperty(value = "变更时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime changeTime;

        @ApiModelProperty(value = "变更影响分析")
        private ChangeImpact impact;
    }

    /**
     * 变更影响分析
     */
    @Data
    @ApiModel(description = "变更影响分析")
    public static class ChangeImpact {

        @ApiModelProperty(value = "影响范围", example = "局部影响")
        private String scope;

        @ApiModelProperty(value = "风险等级", example = "LOW")
        private String riskLevel;

        @ApiModelProperty(value = "影响节点列表")
        private List<String> affectedNodes;

        @ApiModelProperty(value = "影响验证规则列表")
        private List<String> affectedValidations;

        @ApiModelProperty(value = "需要重新测试", example = "true")
        private Boolean requiresRetest;

        @ApiModelProperty(value = "影响说明")
        private String impactDescription;
    }

    /**
     * 依赖关系信息
     */
    @Data
    @ApiModel(description = "依赖关系信息")
    public static class DependencyInfo {

        @ApiModelProperty(value = "依赖ID", example = "dep_001")
        private String dependencyId;

        @ApiModelProperty(value = "依赖类型", example = "PARENT_VERSION")
        private String type;

        @ApiModelProperty(value = "依赖对象ID", example = "1")
        private String objectId;

        @ApiModelProperty(value = "依赖对象类型", example = "VERSION")
        private String objectType;

        @ApiModelProperty(value = "依赖对象名称", example = "v1.0.0")
        private String objectName;

        @ApiModelProperty(value = "依赖描述", example = "基于v1.0.0版本修改")
        private String description;

        @ApiModelProperty(value = "是否强依赖", example = "true")
        private Boolean strongDependency;

        @ApiModelProperty(value = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;
    }

    /**
     * 相关文件信息
     */
    @Data
    @ApiModel(description = "相关文件信息")
    public static class RelatedFile {

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

        @ApiModelProperty(value = "文件描述", example = "工作流定义文件")
        private String description;

        @ApiModelProperty(value = "MD5哈希值", example = "d41d8cd98f00b204e9800998ecf8427e")
        private String md5Hash;

        @ApiModelProperty(value = "版本信息")
        private String versionInfo;

        @ApiModelProperty(value = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;

        @ApiModelProperty(value = "创建人", example = "admin")
        private String createdBy;
    }
}