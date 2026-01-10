package com.workflow.platform.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

//模式类型枚举
@Getter
public enum ModeType {
    ONLINE("online", "在线模式"),
    OFFLINE("offline", "离线模式"),
    MIXED("mixed", "混合模式");

    private final String code;
    private final String description;

    ModeType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static ModeType fromCode(String code) {
        for (ModeType mode : ModeType.values()) {
            if (mode.getCode().equalsIgnoreCase(code)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("未知的模式类型: " + code);
    }
}