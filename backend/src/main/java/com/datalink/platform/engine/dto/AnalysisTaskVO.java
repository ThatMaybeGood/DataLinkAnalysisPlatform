package com.datalink.platform.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分析任务列表项（不含草稿快照大列）
 */
@Data
public class AnalysisTaskVO {
    private Long id;
    private Long connectorId;
    private String connectorIds;
    private String connectorName;
    private String taskType;
    private String status;
    private String errorMessage;
    private String operator;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
