package com.workflow.platform.model.vo;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:22
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 冲突记录视图对象
 */
@Data
@ApiModel(description = "冲突记录视图对象")
public class ConflictRecordVO {

    @ApiModelProperty(value = "冲突记录ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "冲突类型", example = "WORKFLOW_CONFLICT")
    private String conflictType;

    @ApiModelProperty(value = "冲突类型名称", example = "工作流冲突")
    private String conflictTypeName;

    @ApiModelProperty(value = "对象类型", example = "WORKFLOW")
    private String objectType;

    @ApiModelProperty(value = "对象类型名称", example = "工作流")
    private String objectTypeName;

    @ApiModelProperty(value = "对象ID", example = "1")
    private String objectId;

    @ApiModelProperty(value = "对象名称", example = "订单处理流程")
    private String objectName;

    @ApiModelProperty(value = "本地数据摘要")
    private Map<String, Object> localDataSummary;

    @ApiModelProperty(value = "远程数据摘要")
    private Map<String, Object> remoteDataSummary;

    @ApiModelProperty(value = "本地版本号", example = "1")
    private Integer localVersion;

    @ApiModelProperty(value = "远程版本号", example = "2")
    private Integer remoteVersion;

    @ApiModelProperty(value = "本地更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime localUpdateTime;

    @ApiModelProperty(value = "远程更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime remoteUpdateTime;

    @ApiModelProperty(value = "冲突解决状态", example = "PENDING")
    private String status;

    @ApiModelProperty(value = "状态名称", example = "待解决")
    private String statusName;

    @ApiModelProperty(value = "解决策略", example = "MANUAL")
    private String resolutionStrategy;

    @ApiModelProperty(value = "解决策略名称", example = "手动解决")
    private String resolutionStrategyName;

    @ApiModelProperty(value = "解决结果数据")
    private Map<String, Object> resolutionResult;

    @ApiModelProperty(value = "解决人", example = "admin")
    private String resolvedBy;

    @ApiModelProperty(value = "解决人姓名", example = "系统管理员")
    private String resolvedByName;

    @ApiModelProperty(value = "解决时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedTime;

    @ApiModelProperty(value = "解决备注", example = "采用客户端优先策略")
    private String resolutionNotes;

    @ApiModelProperty(value = "同步任务ID", example = "sync_001")
    private String syncTaskId;

    @ApiModelProperty(value = "设备/客户端标识", example = "web_client_001")
    private String clientId;

    @ApiModelProperty(value = "客户端信息")
    private ClientInfo clientInfo;

    @ApiModelProperty(value = "IP地址", example = "192.168.1.100")
    private String ipAddress;

    @ApiModelProperty(value = "冲突检测时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime detectedTime;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @ApiModelProperty(value = "元数据")
    private Map<String, Object> metadata;

    @ApiModelProperty(value = "冲突详细描述", example = "工作流配置被不同用户同时修改")
    private String description;

    @ApiModelProperty(value = "冲突严重程度", example = "MEDIUM")
    private String severity;

    @ApiModelProperty(value = "严重程度名称", example = "中等")
    private String severityName;

    @ApiModelProperty(value = "是否已通知", example = "false")
    private Boolean notified;

    @ApiModelProperty(value = "重试次数", example = "0")
    private Integer retryCount;

    @ApiModelProperty(value = "最后重试时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastRetryTime;

    @ApiModelProperty(value = "是否自动解决", example = "false")
    private Boolean autoResolved;

    @ApiModelProperty(value = "差异详情")
    private List<ConflictDiffDetail> diffDetails;

    @ApiModelProperty(value = "冲突影响分析")
    private ConflictImpactAnalysis impactAnalysis;

    @ApiModelProperty(value = "建议解决策略")
    private List<SuggestedResolutionStrategy> suggestedStrategies;

    @ApiModelProperty(value = "是否可以自动解决", example = "true")
    private Boolean canAutoResolve;

    @ApiModelProperty(value = "是否可忽略", example = "true")
    private Boolean canIgnore;

    @ApiModelProperty(value = "是否可重新检测", example = "true")
    private Boolean canRetry;

    /**
     * 客户端信息
     */
    @Data
    @ApiModel(description = "客户端信息")
    public static class ClientInfo {

        @ApiModelProperty(value = "客户端类型", example = "WEB")
        private String clientType;

        @ApiModelProperty(value = "客户端版本", example = "1.0.0")
        private String clientVersion;

        @ApiModelProperty(value = "操作系统", example = "Windows 10")
        private String operatingSystem;

        @ApiModelProperty(value = "浏览器信息", example = "Chrome 120.0.0.0")
        private String browserInfo;

        @ApiModelProperty(value = "用户代理", example = "Mozilla/5.0...")
        private String userAgent;
    }

    /**
     * 冲突差异详情
     */
    @Data
    @ApiModel(description = "冲突差异详情")
    public static class ConflictDiffDetail {

        @ApiModelProperty(value = "字段路径", example = "workflow.name")
        private String fieldPath;

        @ApiModelProperty(value = "字段名称", example = "工作流名称")
        private String fieldName;

        @ApiModelProperty(value = "字段类型", example = "STRING")
        private String fieldType;

        @ApiModelProperty(value = "本地值", example = "订单处理流程_v1")
        private Object localValue;

        @ApiModelProperty(value = "远程值", example = "订单处理流程_v2")
        private Object remoteValue;

        @ApiModelProperty(value = "差异类型", example = "VALUE_CHANGED")
        private String diffType;

        @ApiModelProperty(value = "差异级别", example = "MEDIUM")
        private String diffLevel;

        @ApiModelProperty(value = "是否冲突", example = "true")
        private Boolean conflict;

        @ApiModelProperty(value = "冲突描述", example = "工作流名称被修改")
        private String description;

        @ApiModelProperty(value = "建议值")
        private Object suggestedValue;

        @ApiModelProperty(value = "建议操作", example = "USE_LOCAL")
        private String suggestedAction;
    }

    /**
     * 冲突影响分析
     */
    @Data
    @ApiModel(description = "冲突影响分析")
    public static class ConflictImpactAnalysis {

        @ApiModelProperty(value = "影响范围", example = "局部影响")
        private String impactScope;

        @ApiModelProperty(value = "影响级别", example = "MEDIUM")
        private String impactLevel;

        @ApiModelProperty(value = "业务影响", example = "中等")
        private String businessImpact;

        @ApiModelProperty(value = "数据一致性风险", example = "低")
        private String consistencyRisk;

        @ApiModelProperty(value = "受影响用户数", example = "5")
        private Integer affectedUsers;

        @ApiModelProperty(value = "受影响工作流数", example = "1")
        private Integer affectedWorkflows;

        @ApiModelProperty(value = "预计解决时间（分钟）", example = "30")
        private Integer estimatedResolutionTime;

        @ApiModelProperty(value = "解决复杂度", example = "MEDIUM")
        private String resolutionComplexity;

        @ApiModelProperty(value = "建议优先级", example = "MEDIUM")
        private String suggestedPriority;

        @ApiModelProperty(value = "影响说明")
        private String impactDescription;

        @ApiModelProperty(value = "风险说明")
        private String riskDescription;

        @ApiModelProperty(value = "解决建议")
        private List<String> resolutionSuggestions;
    }

    /**
     * 建议解决策略
     */
    @Data
    @ApiModel(description = "建议解决策略")
    public static class SuggestedResolutionStrategy {

        @ApiModelProperty(value = "策略类型", example = "CLIENT_PRIORITY")
        private String strategyType;

        @ApiModelProperty(value = "策略名称", example = "客户端优先")
        private String strategyName;

        @ApiModelProperty(value = "策略描述", example = "使用本地版本覆盖远程版本")
        private String strategyDescription;

        @ApiModelProperty(value = "适用场景", example = "本地修改更重要")
        private String applicableScenario;

        @ApiModelProperty(value = "风险等级", example = "LOW")
        private String riskLevel;

        @ApiModelProperty(value = "解决成功率", example = "95.5")
        private Double successRate;

        @ApiModelProperty(value = "推荐指数", example = "4.5")
        private Double recommendationScore;

        @ApiModelProperty(value = "自动解决支持", example = "true")
        private Boolean autoResolutionSupported;

        @ApiModelProperty(value = "预期结果")
        private String expectedOutcome;
    }
}

