package com.workflow.platform.model.dto;

import com.workflow.platform.enums.ConflictResolutionType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 冲突解决请求数据传输对象
 */
@Data
@ApiModel(description = "冲突解决请求数据传输对象")
public class ConflictResolutionDTO {

    @ApiModelProperty(value = "冲突记录ID", required = true, example = "1")
    @NotNull(message = "冲突记录ID不能为空")
    private Long conflictId;

    @ApiModelProperty(value = "解决策略", required = true, example = "CLIENT_PRIORITY")
    @NotBlank(message = "解决策略不能为空")
    private String resolutionStrategy;

    @ApiModelProperty(value = "解决策略类型", required = true)
    @NotNull(message = "解决策略类型不能为空")
    private ConflictResolutionType resolutionType;

    @ApiModelProperty(value = "解决结果数据")
    private Map<String, Object> resolutionData;

    @ApiModelProperty(value = "选择的数据字段（用于手动合并）")
    private Map<String, String> selectedFields;

    @ApiModelProperty(value = "自定义解决逻辑")
    private String customResolutionLogic;

    @ApiModelProperty(value = "解决备注", example = "采用客户端优先策略，因为本地修改更符合业务需求")
    private String resolutionNotes;

    @ApiModelProperty(value = "解决人", example = "admin")
    private String resolvedBy;

    @ApiModelProperty(value = "解决人姓名", example = "系统管理员")
    private String resolvedByName;

    @ApiModelProperty(value = "是否通知相关人员", example = "true")
    private Boolean notifyUsers = true;

    @ApiModelProperty(value = "是否创建解决记录", example = "true")
    private Boolean createResolutionRecord = true;

    @ApiModelProperty(value = "是否强制执行", example = "false")
    private Boolean forceResolution = false;

    @ApiModelProperty(value = "解决时间（ISO格式）", example = "2024-01-01T10:00:00")
    private String resolutionTime;

    @ApiModelProperty(value = "验证令牌")
    private String validationToken;

    @ApiModelProperty(value = "解决会话ID")
    private String sessionId;

    @ApiModelProperty(value = "客户端信息")
    private ClientInfo clientInfo;

    @ApiModelProperty(value = "审核信息")
    private AuditInfo auditInfo;

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

        @ApiModelProperty(value = "IP地址", example = "192.168.1.100")
        private String ipAddress;

        @ApiModelProperty(value = "用户代理", example = "Mozilla/5.0...")
        private String userAgent;

        @ApiModelProperty(value = "设备标识", example = "device_001")
        private String deviceId;

        @ApiModelProperty(value = "屏幕分辨率", example = "1920x1080")
        private String screenResolution;
    }

    /**
     * 审核信息
     */
    @Data
    @ApiModel(description = "审核信息")
    public static class AuditInfo {

        @ApiModelProperty(value = "审核人", example = "auditor_001")
        private String auditor;

        @ApiModelProperty(value = "审核意见", example = "同意解决方案")
        private String auditOpinion;

        @ApiModelProperty(value = "审核级别", example = "LEVEL_1")
        private String auditLevel;

        @ApiModelProperty(value = "是否需要审核", example = "true")
        private Boolean requiresAudit;

        @ApiModelProperty(value = "审核流程ID", example = "audit_flow_001")
        private String auditFlowId;

        @ApiModelProperty(value = "审核节点", example = "FIRST_REVIEW")
        private String auditNode;
    }

    /**
     * 冲突解决结果
     */
    @Data
    @ApiModel(description = "冲突解决结果")
    public static class ResolutionResult {

        @ApiModelProperty(value = "解决是否成功", example = "true")
        private Boolean success;

        @ApiModelProperty(value = "解决后的数据ID", example = "data_001")
        private String resolvedDataId;

        @ApiModelProperty(value = "解决版本号", example = "v2.0.0")
        private String resolvedVersion;

        @ApiModelProperty(value = "解决时间戳", example = "1704067200000")
        private Long resolutionTimestamp;

        @ApiModelProperty(value = "解决摘要")
        private String resolutionSummary;

        @ApiModelProperty(value = "解决详情")
        private Map<String, Object> resolutionDetails;

        @ApiModelProperty(value = "受影响的关联对象")
        private List<String> affectedObjects;

        @ApiModelProperty(value = "后续操作建议")
        private List<String> nextActions;

        @ApiModelProperty(value = "风险评估", example = "LOW")
        private String riskAssessment;

        @ApiModelProperty(value = "解决质量评分", example = "95")
        private Integer qualityScore;
    }
}

/**
 * 批量冲突解决请求DTO
 */
@Data
@ApiModel(description = "批量冲突解决请求数据传输对象")
class BatchConflictResolutionDTO {

    @ApiModelProperty(value = "冲突解决请求列表", required = true)
    @NotNull(message = "解决请求列表不能为空")
    private List<ConflictResolutionDTO> resolutions;

    @ApiModelProperty(value = "批量操作ID", example = "batch_001")
    private String batchId;

    @ApiModelProperty(value = "批量操作描述", example = "批量解决工作流冲突")
    private String batchDescription;

    @ApiModelProperty(value = "是否异步执行", example = "true")
    private Boolean asyncExecution = false;

    @ApiModelProperty(value = "回调地址")
    private String callbackUrl;

    @ApiModelProperty(value = "超时时间（秒）", example = "300")
    private Integer timeoutSeconds;
}

/**
 * 冲突解决验证DTO
 */
@Data
@ApiModel(description = "冲突解决验证数据传输对象")
class ConflictResolutionValidationDTO {

    @ApiModelProperty(value = "冲突记录ID", required = true, example = "1")
    @NotNull(message = "冲突记录ID不能为空")
    private Long conflictId;

    @ApiModelProperty(value = "解决策略", required = true, example = "CLIENT_PRIORITY")
    @NotBlank(message = "解决策略不能为空")
    private String resolutionStrategy;

    @ApiModelProperty(value = "预计解决结果")
    private Map<String, Object> expectedResult;

    @ApiModelProperty(value = "验证规则")
    private List<String> validationRules;

    @ApiModelProperty(value = "验证超时时间", example = "30")
    private Integer validationTimeout;
}