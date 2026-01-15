package com.workflow.platform.service.impl;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/16 00:05
 */
@Data
public class ExecutionRecord {
    private String id;
    private String workflowId;
    private Object parameters;
    private String status;
    private Long startTime;
    private Long endTime;
    private Object result;
    private String errorMessage;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
