package com.datalink.platform.datasource.dialect;

/**
 * H2 内存方言：host/port 忽略，database 视为内存库名；双引号引用标识符。
 */
public class H2Dialect implements DbDialect {

    @Override
    public String driverClass() {
        return "org.h2.Driver";
    }

    @Override
    public String buildJdbcUrl(String host, int port, String database) {
        return "jdbc:h2:mem:" + database + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
    }

    @Override
    public String testSql() {
        return "SELECT 1";
    }

    @Override
    public String quote(String id) {
        return "\"" + id + "\"";
    }

    @Override
    public String previewSql(String table, int limit) {
        return "SELECT * FROM " + quote(table) + " LIMIT " + limit;
    }
}
