package com.workflow.platform.model.dto;

import com.workflow.platform.enums.SyncStatus;
import lombok.Data;

import java.util.Map;

/**
 * 同步任务数据传输对象
 */
@Data
public class SyncTaskDTO {
    private String id;
    private String type; // workflow, node, rule, etc.
    private Object data;
    private long priority;
    private SyncStatus status;
    private int retryCount;
    private long estimatedTime; // 估计执行时间（毫秒）
    private long timeout; // 超时时间（毫秒）
    private String errorMessage;
    private String result;
    private long processingTime;
    private long createdTime;
    private long startTime;
    private long endTime;
    private Map<String, Object> metadata;

    // 任务类型常量
    public static final String TYPE_WORKFLOW = "workflow";
    public static final String TYPE_NODE = "node";
    public static final String TYPE_RULE = "rule";
    public static final String TYPE_USER = "user";
    public static final String TYPE_CATEGORY = "category";

    // 优先级常量
    public static final long PRIORITY_HIGH = 1;
    public static final long PRIORITY_NORMAL = 1000;
    public static final long PRIORITY_LOW = 10000;
}