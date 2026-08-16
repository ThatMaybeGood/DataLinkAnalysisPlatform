package com.datalink.platform.monitor.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警视图对象
 */
@Data
public class AlertVO {
    private String id;
    /** 告警类型 STUCK/FAIL/TIMEOUT/CHECK_FAIL */
    private String type;
    private String severity;
    /** NODE/INSTANCE/ROUTE/PROCESS */
    private String targetType;
    /** 目标对象名 */
    private String targetName;
    private String message;
    /** OPEN/RESOLVED */
    private String status;
    /** 告警级别 L1-L4 */
    private String level;
    /** 告警时间 */
    private LocalDateTime time;
    /** 解决时间 */
    private LocalDateTime resolvedAt;
}
