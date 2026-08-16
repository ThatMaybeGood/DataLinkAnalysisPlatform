package com.datalink.platform.monitor.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建工单请求
 */
@Data
public class CreateTicketRequest {
    /** 关联告警 */
    @NotNull
    private Long alertId;
    /** 处理人（可空） */
    private String assignee;
    /** 工单描述（可空） */
    private String description;
}
