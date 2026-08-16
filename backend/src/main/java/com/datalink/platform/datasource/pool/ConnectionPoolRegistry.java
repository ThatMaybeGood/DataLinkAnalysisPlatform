package com.datalink.platform.datasource.pool;

import com.datalink.platform.datasource.dialect.DbDialect;
import com.datalink.platform.datasource.dialect.DbDialectFactory;
import com.datalink.platform.datasource.entity.Connector;
import com.datalink.platform.datasource.util.AesUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HikariCP 连接池注册表。
 * 按连接器 id 懒加载连接池，参数变化时调用 evict 失效重建，closeAll 用于关闭回收。
 */
@Component
public class ConnectionPoolRegistry {

    private final Map<Long, HikariDataSource> pools = new ConcurrentHashMap<>();
    private final AesUtil aesUtil;

    public ConnectionPoolRegistry(AesUtil aesUtil) {
        this.aesUtil = aesUtil;
    }

    /** 懒加载获取连接池；参数变化调用 evict 后重建。 */
    public DataSource get(Connector c) {
        return pools.computeIfAbsent(c.getId(), id -> build(c));
    }

    /** 移除并关闭指定连接池。 */
    public void evict(Long id) {
        HikariDataSource ds = pools.remove(id);
        if (ds != null) ds.close();
    }

    /** 关闭全部连接池并清空。 */
    public void closeAll() {
        pools.values().forEach(HikariDataSource::close);
        pools.clear();
    }

    private HikariDataSource build(Connector c) {
        DbDialect d = DbDialectFactory.ofCode(c.getDbType());
        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName(d.driverClass());
        cfg.setJdbcUrl(d.buildJdbcUrl(c.getHost(), c.getPort() == null ? 0 : c.getPort(), c.getDatabaseName()));
        cfg.setUsername(c.getUsername());
        cfg.setPassword(aesUtil.decrypt(c.getEncryptedPwd()));
        cfg.setPoolName("datalink-conn-" + c.getId());
        cfg.setMaximumPoolSize(5);
        cfg.setConnectionTimeout(10_000);
        cfg.setValidationTimeout(5_000);
        return new HikariDataSource(cfg);
    }
}
