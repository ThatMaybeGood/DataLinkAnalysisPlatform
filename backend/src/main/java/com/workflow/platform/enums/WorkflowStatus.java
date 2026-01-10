package com.workflow.platform.enums;

import lombok.Getter;
//工作流状态枚举
@Getter
public enum WorkflowStatus {
    DRAFT("draft", "草稿"),
    ACTIVE("active", "活跃"),
    INACTIVE("inactive", "不活跃"),
    ARCHIVED("archived", "已归档"),
    TESTING("testing", "测试中");

    private final String code;
    private final String description;

    WorkflowStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}