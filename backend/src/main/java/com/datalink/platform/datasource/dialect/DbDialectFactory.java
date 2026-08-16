package com.datalink.platform.datasource.dialect;

import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.datasource.ConnectorDbType;

/**
 * 方言工厂：按 {@link ConnectorDbType} 返回对应方言实现。
 */
public final class DbDialectFactory {

    private DbDialectFactory() {
    }

    public static DbDialect of(ConnectorDbType type) {
        switch (type) {
            case MYSQL:
                return new MySqlDialect();
            case POSTGRESQL:
                return new PostgreSqlDialect();
            case H2:
                return new H2Dialect();
            default:
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "未支持的数据库类型");
        }
    }

    public static DbDialect ofCode(String code) {
        return of(ConnectorDbType.from(code));
    }
}
