package com.workflow.platform.model.vo;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:09
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 版本比较视图对象
 *
 * 5. VersionComparisonVO
 *
 * 用于版本比较功能
 * 包含详细的差异分析和影响评估
 * 提供智能的建议操作
 *
 *
 */
@Data
@ApiModel(description = "版本比较视图对象")
public class VersionComparisonVO {

    @ApiModelProperty(value = "比较ID", example = "compare_001")
    private String comparisonId;

    @ApiModelProperty(value = "比较时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime comparisonTime;

    @ApiModelProperty(value = "版本1信息")
    private ComparedVersion version1;

    @ApiModelProperty(value = "版本2信息")
    private ComparedVersion version2;

    @ApiModelProperty(value = "总体差异统计")
    private DiffStatistics overallStatistics;

    @ApiModelProperty(value = "节点差异列表")
    private List<NodeDiff> nodeDiffs;

    @ApiModelProperty(value = "验证规则差异列表")
    private List<ValidationDiff> validationDiffs;

    @ApiModelProperty(value = "连接线差异列表")
    private List<ConnectionDiff> connectionDiffs;

    @ApiModelProperty(value = "配置差异列表")
    private List<ConfigDiff> configDiffs;

    @ApiModelProperty(value = "参数差异列表")
    private List<ParameterDiff> parameterDiffs;

    @ApiModelProperty(value = "元数据差异列表")
    private List<MetadataDiff> metadataDiffs;

    @ApiModelProperty(value = "变更影响分析")
    private ComparisonImpact impactAnalysis;

    @ApiModelProperty(value = "建议操作")
    private List<SuggestedAction> suggestedActions;

    /**
     * 比较版本信息
     */
    @Data
    @ApiModel(description = "比较版本信息")
    public static class ComparedVersion {

        @ApiModelProperty(value = "版本ID", example = "1")
        private Long versionId;

        @ApiModelProperty(value = "版本号", example = "1")
        private Integer versionNumber;

        @ApiModelProperty(value = "版本名称", example = "v1.0.0")
        private String versionName;

        @ApiModelProperty(value = "创建人", example = "admin")
        private String createdBy;

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

        @ApiModelProperty(value = "节点数量", example = "10")
        private Integer nodeCount;

        @ApiModelProperty(value = "验证规则数量", example = "5")
        private Integer validationCount;
    }

    /**
     * 差异统计信息
     */
    @Data
    @ApiModel(description = "差异统计信息")
    public static class DiffStatistics {

        @ApiModelProperty(value = "总差异数", example = "15")
        private Integer totalDiffs;

        @ApiModelProperty(value = "新增数量", example = "5")
        private Integer addedCount;

        @ApiModelProperty(value = "删除数量", example = "3")
        private Integer deletedCount;

        @ApiModelProperty(value = "修改数量", example = "7")
        private Integer modifiedCount;

        @ApiModelProperty(value = "移动数量", example = "0")
        private Integer movedCount;

        @ApiModelProperty(value = "节点差异数", example = "8")
        private Integer nodeDiffCount;

        @ApiModelProperty(value = "验证规则差异数", example = "3")
        private Integer validationDiffCount;

        @ApiModelProperty(value = "连接线差异数", example = "2")
        private Integer connectionDiffCount;

        @ApiModelProperty(value = "配置差异数", example = "1")
        private Integer configDiffCount;

        @ApiModelProperty(value = "参数差异数", example = "1")
        private Integer parameterDiffCount;

        @ApiModelProperty(value = "差异复杂度", example = "MEDIUM")
        private String complexity;

        @ApiModelProperty(value = "合并冲突数", example = "2")
        private Integer conflictCount;
    }

    /**
     * 节点差异
     */
    @Data
    @ApiModel(description = "节点差异")
    public static class NodeDiff {

        @ApiModelProperty(value = "节点ID", example = "node_001")
        private String nodeId;

        @ApiModelProperty(value = "节点名称", example = "开始节点")
        private String nodeName;

        @ApiModelProperty(value = "差异类型", example = "ADDED", allowableValues = "ADDED,DELETED,MODIFIED,MOVED")
        private String diffType;

        @ApiModelProperty(value = "差异描述", example = "新增了开始节点")
        private String description;

        @ApiModelProperty(value = "版本1中的值")
        private Map<String, Object> valueInVersion1;

        @ApiModelProperty(value = "版本2中的值")
        private Map<String, Object> valueInVersion2;

        @ApiModelProperty(value = "差异详情")
        private List<FieldDiff> fieldDiffs;

        @ApiModelProperty(value = "影响分析")
        private DiffImpact impact;

        @ApiModelProperty(value = "建议操作", example = "KEEP_BOTH")
        private String suggestedAction;
    }

    /**
     * 验证规则差异
     */
    @Data
    @ApiModel(description = "验证规则差异")
    public static class ValidationDiff {

        @ApiModelProperty(value = "规则ID", example = "rule_001")
        private String ruleId;

        @ApiModelProperty(value = "规则名称", example = "必填验证")
        private String ruleName;

        @ApiModelProperty(value = "差异类型", example = "MODIFIED")
        private String diffType;

        @ApiModelProperty(value = "差异描述", example = "修改了验证条件")
        private String description;

        @ApiModelProperty(value = "版本1中的值")
        private Map<String, Object> valueInVersion1;

        @ApiModelProperty(value = "版本2中的值")
        private Map<String, Object> valueInVersion2;

        @ApiModelProperty(value = "差异详情")
        private List<FieldDiff> fieldDiffs;

        @ApiModelProperty(value = "影响分析")
        private DiffImpact impact;
    }

    /**
     * 连接线差异
     */
    @Data
    @ApiModel(description = "连接线差异")
    public static class ConnectionDiff {

        @ApiModelProperty(value = "连接线ID", example = "conn_001")
        private String connectionId;

        @ApiModelProperty(value = "标签", example = "成功")
        private String label;

        @ApiModelProperty(value = "差异类型", example = "DELETED")
        private String diffType;

        @ApiModelProperty(value = "差异描述", example = "删除了连接线")
        private String description;

        @ApiModelProperty(value = "版本1中的值")
        private Map<String, Object> valueInVersion1;

        @ApiModelProperty(value = "版本2中的值")
        private Map<String, Object> valueInVersion2;

        @ApiModelProperty(value = "差异详情")
        private List<FieldDiff> fieldDiffs;

        @ApiModelProperty(value = "影响分析")
        private DiffImpact impact;
    }

    /**
     * 配置差异
     */
    @Data
    @ApiModel(description = "配置差异")
    public static class ConfigDiff {

        @ApiModelProperty(value = "配置键", example = "timeout")
        private String configKey;

        @ApiModelProperty(value = "配置名称", example = "超时时间")
        private String configName;

        @ApiModelProperty(value = "差异类型", example = "MODIFIED")
        private String diffType;

        @ApiModelProperty(value = "差异描述", example = "修改了超时时间")
        private String description;

        @ApiModelProperty(value = "版本1中的值", example = "3600")
        private Object valueInVersion1;

        @ApiModelProperty(value = "版本2中的值", example = "7200")
        private Object valueInVersion2;

        @ApiModelProperty(value = "影响分析")
        private DiffImpact impact;
    }

    /**
     * 参数差异
     */
    @Data
    @ApiModel(description = "参数差异")
    public static class ParameterDiff {

        @ApiModelProperty(value = "参数ID", example = "param_001")
        private String paramId;

        @ApiModelProperty(value = "参数名称", example = "userName")
        private String paramName;

        @ApiModelProperty(value = "差异类型", example = "ADDED")
        private String diffType;

        @ApiModelProperty(value = "差异描述", example = "新增了用户名字段")
        private String description;

        @ApiModelProperty(value = "版本1中的值")
        private Map<String, Object> valueInVersion1;

        @ApiModelProperty(value = "版本2中的值")
        private Map<String, Object> valueInVersion2;

        @ApiModelProperty(value = "差异详情")
        private List<FieldDiff> fieldDiffs;

        @ApiModelProperty(value = "影响分析")
        private DiffImpact impact;
    }

    /**
     * 元数据差异
     */
    @Data
    @ApiModel(description = "元数据差异")
    public static class MetadataDiff {

        @ApiModelProperty(value = "元数据键", example = "author")
        private String metadataKey;

        @ApiModelProperty(value = "元数据名称", example = "作者")
        private String metadataName;

        @ApiModelProperty(value = "差异类型", example = "MODIFIED")
        private String diffType;

        @ApiModelProperty(value = "差异描述", example = "修改了作者信息")
        private String description;

        @ApiModelProperty(value = "版本1中的值", example = "admin")
        private Object valueInVersion1;

        @ApiModelProperty(value = "版本2中的值", example = "user001")
        private Object valueInVersion2;
    }

    /**
     * 字段差异
     */
    @Data
    @ApiModel(description = "字段差异")
    public static class FieldDiff {

        @ApiModelProperty(value = "字段名称", example = "name")
        private String fieldName;

        @ApiModelProperty(value = "字段显示名", example = "名称")
        private String fieldDisplayName;

        @ApiModelProperty(value = "字段类型", example = "STRING")
        private String fieldType;

        @ApiModelProperty(value = "版本1中的值")
        private Object oldValue;

        @ApiModelProperty(value = "版本2中的值")
        private Object newValue;

        @ApiModelProperty(value = "差异类型", example = "CHANGED")
        private String diffType;

        @ApiModelProperty(value = "差异级别", example = "MINOR")
        private String diffLevel;

        @ApiModelProperty(value = "差异描述", example = "名称从'开始'改为'启动'")
        private String description;

        @ApiModelProperty(value = "是否冲突", example = "false")
        private Boolean conflict;
    }

    /**
     * 差异影响分析
     */
    @Data
    @ApiModel(description = "差异影响分析")
    public static class DiffImpact {

        @ApiModelProperty(value = "影响范围", example = "局部影响")
        private String scope;

        @ApiModelProperty(value = "风险等级", example = "LOW")
        private String riskLevel;

        @ApiModelProperty(value = "影响节点列表")
        private List<String> affectedNodes;

        @ApiModelProperty(value = "影响验证规则列表")
        private List<String> affectedValidations;

        @ApiModelProperty(value = "是否需要测试", example = "true")
        private Boolean requiresTesting;

        @ApiModelProperty(value = "影响说明")
        private String impactDescription;

        @ApiModelProperty(value = "建议措施")
        private String suggestedAction;
    }

    /**
     * 比较影响分析
     */
    @Data
    @ApiModel(description = "比较影响分析")
    public static class ComparisonImpact {

        @ApiModelProperty(value = "总体风险等级", example = "MEDIUM")
        private String overallRiskLevel;

        @ApiModelProperty(value = "合并复杂度", example = "MEDIUM")
        private String mergeComplexity;

        @ApiModelProperty(value = "建议合并策略", example = "SELECTIVE_MERGE")
        private String suggestedMergeStrategy;

        @ApiModelProperty(value = "冲突数量", example = "2")
        private Integer conflictCount;

        @ApiModelProperty(value = "测试建议")
        private List<String> testingSuggestions;

        @ApiModelProperty(value = "部署建议")
        private List<String> deploymentSuggestions;

        @ApiModelProperty(value = "回滚风险评估", example = "LOW")
        private String rollbackRisk;
    }

    /**
     * 建议操作
     */
    @Data
    @ApiModel(description = "建议操作")
    public static class SuggestedAction {

        @ApiModelProperty(value = "操作ID", example = "action_001")
        private String actionId;

        @ApiModelProperty(value = "操作类型", example = "MERGE")
        private String actionType;

        @ApiModelProperty(value = "操作描述", example = "合并两个版本的节点修改")
        private String description;

        @ApiModelProperty(value = "优先级", example = "HIGH")
        private String priority;

        @ApiModelProperty(value = "执行步骤")
        private List<String> steps;

        @ApiModelProperty(value = "预期结果")
        private String expectedOutcome;

        @ApiModelProperty(value = "风险评估", example = "LOW")
        private String riskAssessment;

        @ApiModelProperty(value = "是否自动执行", example = "false")
        private Boolean autoExecutable;
    }
}