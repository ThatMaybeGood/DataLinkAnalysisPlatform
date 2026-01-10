package com.workflow.platform.enums;
//同步状态枚举

import lombok.Getter;

@Getter
public enum SyncStatus {
    PENDING("pending", "等待中"),
    RUNNING("running", "执行中"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    CANCELLED("cancelled", "已取消"),
    TIMEOUT("timeout", "超时"),
    CONFLICT("conflict", "冲突");

    private final String code;
    private final String description;

    SyncStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static SyncStatus fromCode(String code) {
        for (SyncStatus status : SyncStatus.values()) {
            if (status.getCode().equalsIgnoreCase(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的同步状态: " + code);
    }
}