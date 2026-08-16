package com.datalink.platform.datasource.dialect;

import com.datalink.platform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 方言工厂测试：类型→实现映射、未知类型异常、MySQL 预览 SQL 引用。
 */
class DbDialectFactoryTest {

    @Test
    void mysql_dialect() {
        assertInstanceOf(MySqlDialect.class, DbDialectFactory.ofCode("mysql"));
    }

    @Test
    void postgres_dialect() {
        assertInstanceOf(PostgreSqlDialect.class, DbDialectFactory.ofCode("postgresql"));
    }

    @Test
    void h2_dialect() {
        assertInstanceOf(H2Dialect.class, DbDialectFactory.ofCode("h2"));
    }

    @Test
    void unknown_throws() {
        assertThrows(BusinessException.class, () -> DbDialectFactory.ofCode("oracle"));
    }

    @Test
    void mysql_preview_quotes() {
        MySqlDialect d = new MySqlDialect();
        assertEquals("SELECT * FROM `t` LIMIT 50", d.previewSql("t", 50));
    }
}
