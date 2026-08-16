package com.datalink.platform.monitor.dto;

import lombok.Data;

/**
 * 更新工单请求（非空字段覆盖）
 */
@Data
public class SaveTicketRequest {
    /** 处理人 */
    private String assignee;
    /** OPEN/PROCESSING/RESOLVED */
    private String status;
    /** 工单描述 */
    private String description;
}
