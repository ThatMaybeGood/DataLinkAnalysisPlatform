package com.workflow.platform.enums;

import lombok.Getter;
//节点类型枚举
@Getter
public enum NodeType {
    START("start", "开始节点"),
    END("end", "结束节点"),
    ACTION("action", "动作节点"),
    DECISION("decision", "决策节点"),
    PARALLEL("parallel", "并行节点"),
    DELAY("delay", "延迟节点"),
    VALIDATION("validation", "验证节点"),
    NOTIFICATION("notification", "通知节点"),
    DATABASE("database", "数据库节点"),
    API("api", "API节点"),
    SCRIPT("script", "脚本节点"),
    CONDITION("condition", "条件节点");

    private final String code;
    private final String description;

    NodeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static NodeType fromCode(String code) {
        for (NodeType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return ACTION; // 默认返回动作节点
    }
}