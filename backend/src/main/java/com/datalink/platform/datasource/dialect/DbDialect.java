package com.datalink.platform.datasource.dialect;

/**
 * 数据库方言可插拔接口。
 * 每种受支持数据库提供驱动、JDBC URL、连通性测试 SQL、标识符引用与数据预览 SQL。
 */
public interface DbDialect {

    /** JDBC 驱动类全名。 */
    String driverClass();

    /** 构建 JDBC URL。H2 方言忽略 host/port。 */
    String buildJdbcUrl(String host, int port, String database);

    /** 连通性测试 SQL。 */
    String testSql();

    /** 标识符（表名/列名）引用转义。 */
    String quote(String identifier);

    /** 数据预览 SQL，如 "SELECT * FROM `t` LIMIT 50"。 */
    String previewSql(String table, int limit);
}
