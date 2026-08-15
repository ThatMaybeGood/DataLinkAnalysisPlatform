package com.datalink.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 上下文冒烟测试：启动完整 Spring 容器。
 * 使用内存 H2（避免与本地文件库 .data/h2 抢锁，测试自包含）。
 * Flyway 自动执行 h2 + common 迁移建表并写入种子数据。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
class DataLinkApplicationTests {

    @Test
    void contextLoads() {
    }
}
