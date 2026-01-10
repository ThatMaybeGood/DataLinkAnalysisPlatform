package com.workflow.platform.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 冲突查询条件DTO
 */
@Data
@ApiModel(description = "冲突查询条件数据传输对象")
public class ConflictQueryDTO {

    @ApiModelProperty(value = "对象类型", example = "WORKFLOW")
    private String objectType;

    @ApiModelProperty(value = "对象ID", example = "1")
    private String objectId;

    @ApiModelProperty(value = "冲突类型", example = "WORKFLOW_CONFLICT")
    private String conflictType;

    @ApiModelProperty(value = "解决状态", example = "PENDING")
    private String status;

    @ApiModelProperty(value = "解决策略", example = "MANUAL")
    private String resolutionStrategy;

    @ApiModelProperty(value = "开始时间")
    private LocalDateTime startTime;

    @ApiModelProperty(value = "结束时间")
    private LocalDateTime endTime;

    @ApiModelProperty(value = "严重程度", example = "HIGH")
    private String severity;

    @ApiModelProperty(value = "是否已通知", example = "true")
    private Boolean notified;

    @ApiModelProperty(value = "是否自动解决", example = "false")
    private Boolean autoResolved;

    @ApiModelProperty(value = "客户端ID", example = "web_client_001")
    private String clientId;

    @ApiModelProperty(value = "页码", example = "1")
    private Integer page = 1;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "排序字段", example = "detectedTime")
    private String sortBy = "detectedTime";

    @ApiModelProperty(value = "排序方向", example = "DESC")
    private String sortDirection = "DESC";
}
