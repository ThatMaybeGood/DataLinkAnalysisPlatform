package com.datalink.platform.engine.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.datasource.entity.Connector;
import com.datalink.platform.datasource.mapper.ConnectorMapper;
import com.datalink.platform.datasource.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 图来源 G3 演示连接器种子（运行期幂等）。
 *
 * <p>Flyway 之后执行：确保存在 database_name=datalink_demo 的 DB 型连接器，指向
 * {@link DemoBizDbInitializer} 创建的内存演示库（sa / secret）。引擎分析默认扫它。
 * 用运行期 AesUtil 加密密码，避免在迁移里硬编码密文。
 */
@Component
@Order(20)
@RequiredArgsConstructor
public class DemoConnectorSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoConnectorSeeder.class);
    private static final String DEMO_PWD = "secret";

    private final ConnectorMapper connectorMapper;
    private final AesUtil aesUtil;

    @Override
    public void run(ApplicationArguments args) {
        Connector existing = connectorMapper.selectOne(Wrappers.lambdaQuery(Connector.class)
                .eq(Connector::getDatabaseName, DemoBizDbInitializer.DEMO_DB)
                .last("LIMIT 1"));
        if (existing == null) {
            Connector c = new Connector();
            c.setConnectorType("DB");
            c.setName("HIS 电子病历演示库");
            c.setDbType("h2");
            c.setDatabaseName(DemoBizDbInitializer.DEMO_DB);
            c.setUsername("sa");
            c.setEncryptedPwd(aesUtil.encrypt(DEMO_PWD));
            c.setEnabled(1);
            c.setIsActive(0);
            connectorMapper.insert(c);
            log.info("[G3] 演示连接器已创建 id={}: {}（{}）", c.getId(), c.getName(), DemoBizDbInitializer.DEMO_DB);
        } else {
            existing.setEncryptedPwd(aesUtil.encrypt(DEMO_PWD));
            existing.setUsername("sa");
            existing.setDbType("h2");
            existing.setEnabled(1);
            connectorMapper.updateById(existing);
            log.info("[G3] 演示连接器已就绪 id={}: {}（{}）", existing.getId(), existing.getName(),
                    DemoBizDbInitializer.DEMO_DB);
        }
    }
}