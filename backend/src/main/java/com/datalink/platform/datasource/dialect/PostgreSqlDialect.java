package com.datalink.platform.datasource.dialect;

/**
 * PostgreSQL 方言：双引号引用标识符。
 */
public class PostgreSqlDialect implements DbDialect {

    @Override
    public String driverClass() {
        return "org.postgresql.Driver";
    }

    @Override
    public String buildJdbcUrl(String host, int port, String database) {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
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
