package com.datalink.platform.datasource.dialect;

/**
 * MySQL 方言：反引号引用标识符。
 */
public class MySqlDialect implements DbDialect {

    @Override
    public String driverClass() {
        return "com.mysql.cj.jdbc.Driver";
    }

    @Override
    public String buildJdbcUrl(String host, int port, String database) {
        return "jdbc:mysql://" + host + ":" + port + "/" + database
                + "?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true";
    }

    @Override
    public String testSql() {
        return "SELECT 1";
    }

    @Override
    public String quote(String id) {
        return "`" + id + "`";
    }

    @Override
    public String previewSql(String table, int limit) {
        return "SELECT * FROM " + quote(table) + " LIMIT " + limit;
    }
}
