package com.datalink.platform.datasource.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.datasource.dto.ConnectorVO;
import com.datalink.platform.datasource.dto.PreviewResult;
import com.datalink.platform.datasource.dto.SaveConnectorRequest;
import com.datalink.platform.datasource.dto.TableInfo;
import com.datalink.platform.datasource.dto.TestResult;
import com.datalink.platform.datasource.entity.Connector;
import com.datalink.platform.datasource.mapper.ConnectorMapper;
import com.datalink.platform.datasource.util.AesUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 连接器服务集成测试（内存 H2 应用库）。
 * 说明：连接器 create 要求非空密码，而 H2 sa 用户密码由应用数据源首次连接决定，
 * 故测试数据源用 sa/secret 创建库，连接器亦用 sa/secret 指向应用库，保证 test/tables/preview 可真实连通。
 * 库名用 datalink_conn_test 与 DataLinkApplicationTests 的 datalink_test 隔离，避免同 JVM 上下文缓存冲突。
 * 每个测试方法 @Transactional 回滚，避免互相污染。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:datalink_conn_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=secret"
})
@Transactional
class ConnectorServiceTest {

    @Autowired
    private ConnectorService service;
    @Autowired
    private ConnectorMapper mapper;
    @Autowired
    private AesUtil aesUtil;

    /** 测试连接器指向应用库，使其能看到 connector 表 */
    private static final String APP_DB = "datalink_conn_test";

    private SaveConnectorRequest buildReq(String name) {
        SaveConnectorRequest req = new SaveConnectorRequest();
        req.setName(name);
        req.setDbType("h2");
        req.setDatabaseName(APP_DB);
        req.setUsername("sa");
        req.setPassword("secret");
        return req;
    }

    @Test
    void create_encrypts_and_no_pwd_in_vo() {
        ConnectorVO vo = service.create(buildReq("加密H2"));
        Connector stored = mapper.selectById(vo.getId());
        assertNotNull(stored);
        // 库中密文 ≠ 明文，可解密还原
        assertNotEquals("secret", stored.getEncryptedPwd());
        assertEquals("secret", aesUtil.decrypt(stored.getEncryptedPwd()));
        // VO 无任何密码字段
        boolean hasPwdField = Arrays.stream(ConnectorVO.class.getDeclaredFields())
                .anyMatch(f -> {
                    String n = f.getName().toLowerCase();
                    return n.contains("password") || n.contains("pwd");
                });
        assertFalse(hasPwdField, "ConnectorVO 不应包含密码字段");
    }

    @Test
    void create_requires_password() {
        SaveConnectorRequest req = new SaveConnectorRequest();
        req.setName("x");
        req.setDbType("h2");
        req.setDatabaseName("y");
        req.setUsername("sa");
        assertThrows(BusinessException.class, () -> service.create(req));
    }

    @Test
    void activate_makes_unique() {
        ConnectorVO a = service.create(buildReq("激活A"));
        ConnectorVO b = service.create(buildReq("激活B"));
        service.activate(a.getId());
        service.activate(b.getId());
        Long activeCount = mapper.selectCount(
                Wrappers.lambdaQuery(Connector.class).eq(Connector::getIsActive, 1));
        assertEquals(1L, activeCount, "激活后 is_active=1 应仅 1 条");
    }

    @Test
    void test_h2_connector_ok() {
        ConnectorVO vo = service.create(buildReq("测试H2"));
        TestResult result = service.test(vo.getId());
        assertTrue(result.isOk(), "H2 连接测试应成功: " + result.getMessage());
        assertNotNull(result.getDbVersion());
        assertTrue(result.getDbVersion().contains("H2"), "dbVersion 应含 H2");
        assertNotNull(result.getLatencyMs());
    }

    @Test
    void tables_and_preview() {
        ConnectorVO vo = service.create(buildReq("浏览H2"));
        List<TableInfo> tables = service.tables(vo.getId());
        assertFalse(tables.isEmpty());
        assertTrue(tables.stream().anyMatch(t -> "connector".equalsIgnoreCase(t.getName())),
                "tables() 应包含 connector 表");
        PreviewResult preview = service.preview(vo.getId(), "connector");
        assertNotNull(preview.getColumns());
        assertFalse(preview.getColumns().isEmpty(), "preview 列不应为空");
        assertNotNull(preview.getRows());
    }
}
