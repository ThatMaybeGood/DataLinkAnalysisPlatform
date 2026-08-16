package com.datalink.platform.datasource;

import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;

/**
 * 数据库连接器类型（可接入的方言枚举）。
 */
public enum ConnectorDbType {

    MYSQL("mysql"),
    POSTGRESQL("postgresql"),
    H2("h2");

    private final String code;

    ConnectorDbType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static ConnectorDbType from(String code) {
        for (ConnectorDbType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "不支持的数据库类型: " + code);
    }
}
