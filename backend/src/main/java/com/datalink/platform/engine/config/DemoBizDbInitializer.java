package com.datalink.platform.engine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 图来源 G3 演示业务库初始化器。
 *
 * <p>启动时把 classpath:engine/his_demo_schema.sql 灌入独立内存库
 * {@code jdbc:h2:mem:datalink_demo;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}。
 * 连接参数刻意附带 DATABASE_TO_LOWER=TRUE，使演示表以全小写标识符落库；
 * 数据池连接器（H2Dialect 无 DATABASE_TO_LOWER，但双引号精确匹配存储大小写）即可
 * 用 {@code "reg_order"} 这类引用正常访问同一内存库。
 *
 * <p>幂等：同一 JVM 内内存库常驻（DB_CLOSE_DELAY=-1），多测试上下文共享 JVM，
 * 若表已存在则跳过，避免二次 CREATE 冲突。
 */
@Component
@Order(10)
public class DemoBizDbInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoBizDbInitializer.class);

    /** 演示库名（连接器 database_name 也指向此值） */
    public static final String DEMO_DB = "datalink_demo";
    private static final String DEMO_URL =
            "jdbc:h2:mem:" + DEMO_DB + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String DEMO_USER = "sa";
    private static final String DEMO_PWD = "secret";
    private static final String SCHEMA_RESOURCE = "engine/his_demo_schema.sql";

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection conn = DriverManager.getConnection(DEMO_URL, DEMO_USER, DEMO_PWD)) {
            boolean exists;
            // 用表名级检查（DATABASE_TO_LOWER 下 schema 名存小写，避免大小写误判重复建表）
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'reg_order'")) {
                rs.next();
                exists = rs.getInt(1) > 0;
            }
            if (exists) {
                log.info("[G3] 演示业务库已存在，跳过建表: mem:{}", DEMO_DB);
                return;
            }
            String sql = new String(Objects.requireNonNull(
                            new ClassPathResource(SCHEMA_RESOURCE).getInputStream()).readAllBytes(),
                    StandardCharsets.UTF_8);
            // 拆句：先剥注释行，再按行末尾分号切分（避免 CREATE 块被文件头注释整块吞掉）
            StringBuilder cur = new StringBuilder();
            List<String> statements = new ArrayList<>();
            for (String line : sql.split("\r?\n")) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("--")) continue;
                cur.append(line).append('\n');
                if (t.endsWith(";")) {
                    statements.add(cur.toString());
                    cur.setLength(0);
                }
            }
            if (cur.toString().trim().length() > 0) statements.add(cur.toString().trim());
            int run = 0;
            try (Statement st = conn.createStatement()) {
                for (String stmt : statements) {
                    st.execute(stmt);
                    run++;
                }
            }
            log.info("[G3] 演示业务库就绪: mem:{} ({} 条语句)", DEMO_DB, run);
        }
    }
}