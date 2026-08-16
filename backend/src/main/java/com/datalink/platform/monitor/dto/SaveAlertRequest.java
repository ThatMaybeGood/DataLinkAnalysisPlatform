package com.datalink.platform.monitor.dto;

import lombok.Data;

/**
 * 创建告警请求
 */
@Data
public class SaveAlertRequest {
    /** 告警类型 STUCK/FAIL/TIMEOUT/CHECK_FAIL */
    private String type;
    /** NODE/INSTANCE/ROUTE/PROCESS */
    private String targetType;
    /** 目标对象 id */
    private Long targetId;
    /** 告警描述 */
    private String message;
    /** 优先级 P1/P2/P3 */
    private String severity;
}
