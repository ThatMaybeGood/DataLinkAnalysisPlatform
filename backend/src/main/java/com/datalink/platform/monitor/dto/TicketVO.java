package com.datalink.platform.monitor.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单视图对象
 */
@Data
public class TicketVO {
    private String id;
    /** 关联告警 id */
    private String alertId;
    /** 处理人 */
    private String assignee;
    private String priority;
    /** OPEN/PROCESSING/RESOLVED */
    private String status;
    private String description;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 解决时间 */
    private LocalDateTime resolvedAt;
}
