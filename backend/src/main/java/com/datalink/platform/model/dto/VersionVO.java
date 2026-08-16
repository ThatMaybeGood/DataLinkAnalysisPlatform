package com.datalink.platform.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配置版本视图对象
 */
@Data
public class VersionVO {
    private String id;
    /** 目标类型 PROCESS/NODE/ROUTE/CHECKPOINT/CONNECTOR */
    private String targetType;
    /** 目标对象名（按 targetType+targetId 反查） */
    private String targetName;
    /** 版本号 */
    private Integer version;
    /** 操作人 */
    private String operator;
    /** 变更说明 */
    private String changeNote;
    /** PUBLISHED 等状态 */
    private String status;
    /** 版本时间 */
    private LocalDateTime time;
}
