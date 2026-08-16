package com.datalink.platform.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 开放 API 上报实例请求
 */
@Data
public class OpenInstanceReport {
    /** 业务单号（必填，作为幂等键） */
    @NotBlank
    private String bizNo;
    /** 业务别名（显示名） */
    private String bizName;
    /** 所属流程 */
    private Long processId;
    /** 选用路线 */
    private Long routeId;
    /** 状态（RUNNING/SUCCESS/FAIL/STUCK/TIMEOUT） */
    private String status;
    /** 当前所处站点 */
    private Long currentNodeId;
}
