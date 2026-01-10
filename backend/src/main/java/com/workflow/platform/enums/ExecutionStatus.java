package com.workflow.platform.enums;
import lombok.Getter;

//执行状态枚举
@Getter
public enum ExecutionStatus {
    PENDING("pending", "待执行"),
    RUNNING("running", "执行中"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    CANCELLED("cancelled", "已取消"),
    TIMEOUT("timeout", "超时"),
    RETRYING("retrying", "重试中");

    private final String code;
    private final String description;

    ExecutionStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}