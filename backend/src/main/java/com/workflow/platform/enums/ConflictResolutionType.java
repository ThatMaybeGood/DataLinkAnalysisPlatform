package com.workflow.platform.enums;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/10 23:47
 */

/**
 * 冲突解决类型枚举
 */
public enum ConflictResolutionType {
    /**
     * 客户端优先：本地修改覆盖服务器修改
     */
    CLIENT_PRIORITY("client_priority", "客户端优先"),

    /**
     * 服务器优先：服务器修改覆盖本地修改
     */
    SERVER_PRIORITY("server_priority", "服务器优先"),

    /**
     * 时间戳优先：使用最新修改
     */
    TIMESTAMP_PRIORITY("timestamp_priority", "时间戳优先"),

    /**
     * 手动解决：需要用户干预
     */
    MANUAL("manual", "手动解决"),

    /**
     * 合并：尝试自动合并修改
     */
    MERGE("merge", "自动合并");

    private final String code;
    private final String description;

    ConflictResolutionType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ConflictResolutionType fromCode(String code) {
        for (ConflictResolutionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的冲突解决类型: " + code);
    }
}